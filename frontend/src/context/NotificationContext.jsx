import React, { createContext, useContext, useEffect, useState, useRef } from 'react';
import { useAuth } from './AuthContext';
import webSocketService from '../services/websocketService';
import {
  getInAppNotifications,
  getUnreadCount,
  markAsRead as apiMarkAsRead,
  markAllAsRead as apiMarkAllAsRead
} from '../services/notifications';
import { notification } from 'antd';

const NotificationContext = createContext();

export function NotificationProvider({ children }) {
  const { currentUser } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  
  // Custom Toast state
  const [toasts, setToasts] = useState([]);
  const toastIdRef = useRef(0);

  // Helper to trigger custom glassmorphic toasts
  const addToast = (notif) => {
    const id = toastIdRef.current++;
    const newToast = {
      id,
      title: notif.title || 'Thông báo mới',
      message: notif.message || '',
      type: notif.type || 'INFO',
    };
    
    setToasts((prev) => [...prev, newToast]);
    
    // Auto-remove toast after 5 seconds
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 5000);
  };

  // Fetch initial notifications and unread count
  const fetchInitialData = async () => {
    if (!currentUser?.accessToken) return;
    
    setLoading(true);
    try {
      const [countRes, listRes] = await Promise.all([
        getUnreadCount(),
        getInAppNotifications(0, 10)
      ]);
      
      setUnreadCount(countRes?.count || 0);
      
      const content = listRes?.content || [];
      setNotifications(content);
      setPage(0);
      setHasMore(!listRes?.last);
    } catch (err) {
      console.error('❌ [NotificationContext] Error fetching initial data:', err);
    } finally {
      setLoading(false);
    }
  };

  // Load more notifications (for pagination / infinite scroll in Notification Center)
  const fetchMoreNotifications = async () => {
    if (loading || !hasMore || !currentUser?.accessToken) return;
    
    setLoading(true);
    try {
      const nextPage = page + 1;
      const res = await getInAppNotifications(nextPage, 10);
      const content = res?.content || [];
      
      setNotifications((prev) => [...prev, ...content]);
      setPage(nextPage);
      setHasMore(!res?.last);
    } catch (err) {
      console.error('❌ [NotificationContext] Error loading more notifications:', err);
    } finally {
      setLoading(false);
    }
  };

  // Mark single notification as read
  const markNotificationRead = async (id) => {
    try {
      await apiMarkAsRead(id);
      
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n))
      );
      
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      console.error('❌ [NotificationContext] Error marking notification as read:', err);
    }
  };

  // Mark all notifications as read
  const markAllNotificationsRead = async () => {
    try {
      await apiMarkAllAsRead();
      
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, readAt: new Date().toISOString() }))
      );
      
      setUnreadCount(0);
    } catch (err) {
      console.error('❌ [NotificationContext] Error marking all notifications as read:', err);
    }
  };

  // Manage WebSocket connection lifecycle
  useEffect(() => {
    if (currentUser?.accessToken) {
      // 1. Fetch initial notifications/counts
      fetchInitialData();
      
      // 2. Setup WebSocket connection
      webSocketService.connect(
        currentUser.accessToken,
        (newNotif) => {
          // Add to notifications state
          setNotifications((prev) => [newNotif, ...prev]);
          // Increment unread count
          setUnreadCount((prev) => prev + 1);
          // Show glassmorphism toast!
          addToast(newNotif);
        },
        (status) => {
          setWsConnected(status);
        }
      );
    } else {
      // Clean up when logging out
      webSocketService.disconnect();
      setNotifications([]);
      setUnreadCount(0);
      setPage(0);
      setHasMore(true);
      setWsConnected(false);
    }

    return () => {
      webSocketService.disconnect();
    };
  }, [currentUser]);

  // Periodic unread count synchronization (every 60s)
  useEffect(() => {
    if (!currentUser?.accessToken) return;

    const interval = setInterval(async () => {
      try {
        const countRes = await getUnreadCount();
        if (countRes && typeof countRes.count === 'number') {
          setUnreadCount(countRes.count);
        }
      } catch (err) {
        console.warn('⚠️ [NotificationContext] Failed to sync unread count periodically:', err);
      }
    }, 60000);

    return () => clearInterval(interval);
  }, [currentUser]);

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        unreadCount,
        hasMore,
        loading,
        wsConnected,
        fetchMoreNotifications,
        fetchInitialData,
        markNotificationRead,
        markAllNotificationsRead,
      }}
    >
      {children}

      {/* RENDER CUSTOM GLASSMORPHIC TOASTS */}
      <div className="custom-toast-container">
        {toasts.map((toast) => {
          // Dynamic styles based on notification type
          // Emerald: SUCCESS, Amber: WARNING, Slate: INFO
          let badgeColor = '#059669'; // Emerald default
          let badgeText = 'Thành công';
          if (toast.type === 'WARNING' || toast.type === 'PAYMENT_FAILED') {
            badgeColor = '#d97706'; // Amber
            badgeText = 'Cảnh báo';
          } else if (toast.type === 'INFO' || toast.type === 'PROMOTION') {
            badgeColor = '#475569'; // Slate
            badgeText = 'Thông tin';
          }

          return (
            <div key={toast.id} className="glass-toast slide-in-toast">
              <div className="toast-header">
                <span className="toast-badge" style={{ backgroundColor: badgeColor }}>
                  {badgeText}
                </span>
                <button 
                  className="toast-close-btn"
                  onClick={() => setToasts((prev) => prev.filter((t) => t.id !== toast.id))}
                >
                  &times;
                </button>
              </div>
              <div className="toast-body">
                <h4 className="toast-title">{toast.title}</h4>
                <p className="toast-message">{toast.message}</p>
              </div>
            </div>
          );
        })}
      </div>
    </NotificationContext.Provider>
  );
}

export const useNotifications = () => useContext(NotificationContext);
