import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscribers = new Set();
  }

  /**
   * Connect to the STOMP server over WebSocket/SockJS
   * @param {string} token - The active JWT access token
   * @param {function} onMessageReceived - Callback when a new notification arrives
   * @param {function} onStatusChange - Callback when connection status changes
   */
  connect(token, onMessageReceived, onStatusChange) {
    if (this.client && this.connected) {
      console.log('🔌 [WebSocket] already connected');
      return;
    }

    if (!token) {
      console.warn('⚠️ [WebSocket] Connect request ignored: No token provided');
      return;
    }

    console.log('🔌 [WebSocket] Initializing connection to /ws/notifications...');

    // Determine target URL. Gateway port is 8080.
    const socketUrl = 'http://localhost:8089/ws/notifications';

    this.client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        console.debug('📡 [STOMP Debug] ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = (frame) => {
      console.log('✅ [WebSocket] Connected successfully!', frame);
      this.connected = true;
      if (onStatusChange) onStatusChange(true);

      // Subscribe to user queue (/user/queue/notifications)
      // Note: Backend routes user-specific queues under '/user/queue/notifications'
      const subscription = this.client.subscribe('/user/queue/notifications', (message) => {
        try {
          const body = JSON.parse(message.body);
          console.log('📦 [WebSocket] Received notification:', body);
          
          // Execute callback
          if (onMessageReceived) {
            onMessageReceived(body);
          }
          
          // Ack receipt
          message.ack();
        } catch (err) {
          console.error('❌ [WebSocket] Error parsing notification body:', err);
        }
      }, { ack: 'client' });

      this.subscribers.add(subscription);
    };

    this.client.onStompError = (frame) => {
      console.error('❌ [WebSocket] STOMP Protocol Error:', frame.headers['message']);
      console.error('Details:', frame.body);
      this.connected = false;
      if (onStatusChange) onStatusChange(false);
    };

    this.client.onWebSocketClose = () => {
      console.warn('⚠️ [WebSocket] Connection closed');
      this.connected = false;
      if (onStatusChange) onStatusChange(false);
    };

    this.client.activate();
  }

  /**
   * Disconnect from STOMP broker
   */
  disconnect() {
    if (this.client) {
      console.log('🔌 [WebSocket] Disconnecting from server...');
      
      // Clean up subscribers
      this.subscribers.forEach((sub) => sub.unsubscribe());
      this.subscribers.clear();

      this.client.deactivate();
      this.client = null;
      this.connected = false;
      console.log('🔌 [WebSocket] Disconnected');
    }
  }
}

const webSocketService = new WebSocketService();
export default webSocketService;
