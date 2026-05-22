import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaCheckDouble, FaCheckCircle, FaExclamationTriangle, FaInfoCircle, FaArrowLeft } from 'react-icons/fa';
import { useNotifications } from '../context/NotificationContext';

export default function NotificationCenter() {
  const navigate = useNavigate();
  const {
    notifications,
    unreadCount,
    hasMore,
    loading,
    fetchMoreNotifications,
    markNotificationRead,
    markAllNotificationsRead,
  } = useNotifications();

  const [activeTab, setActiveTab] = useState('ALL'); // 'ALL', 'ORDERS', 'PAYMENTS'

  const handleNotificationClick = async (notif) => {
    if (!notif.isRead) {
      await markNotificationRead(notif.id);
    }

    if (notif.referenceId) {
      if (notif.type.includes('ORDER') || notif.type.includes('PAYMENT')) {
        navigate(`/order/${notif.referenceId}`);
      }
    }
  };

  const getIcon = (type) => {
    if (type === 'WARNING' || type === 'PAYMENT_FAILED') {
      return <FaExclamationTriangle className="notification-icon" />;
    }
    if (type === 'SUCCESS' || type === 'PAYMENT_SUCCESS') {
      return <FaCheckCircle className="notification-icon" />;
    }
    return <FaInfoCircle className="notification-icon" />;
  };

  const getIconClass = (type) => {
    if (type === 'WARNING' || type === 'PAYMENT_FAILED') return 'warning';
    if (type === 'SUCCESS' || type === 'PAYMENT_SUCCESS') return 'success';
    return 'info';
  };

  const formatTime = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Filter notifications locally based on active tab
  const filteredNotifications = notifications.filter((notif) => {
    if (activeTab === 'ALL') return true;
    if (activeTab === 'ORDERS') return notif.type.includes('ORDER');
    if (activeTab === 'PAYMENTS') return notif.type.includes('PAYMENT');
    return true;
  });

  return (
    <div className="notification-center-container">
      {/* Header */}
      <div className="notification-center-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button 
            onClick={() => navigate(-1)} 
            style={{ 
              background: 'none', 
              border: 'none', 
              cursor: 'pointer', 
              color: '#475569',
              display: 'flex',
              alignItems: 'center'
            }}
            aria-label="Quay lại"
          >
            <FaArrowLeft size={18} />
          </button>
          <h2>Trung tâm thông báo</h2>
        </div>
        {unreadCount > 0 && (
          <button className="mark-all-btn" onClick={markAllNotificationsRead}>
            <FaCheckDouble style={{ marginRight: '6px', display: 'inline' }} />
            Đọc tất cả ({unreadCount})
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="notification-center-tabs">
        <button
          className={`notification-tab-btn ${activeTab === 'ALL' ? 'active' : ''}`}
          onClick={() => setActiveTab('ALL')}
        >
          Tất cả
        </button>
        <button
          className={`notification-tab-btn ${activeTab === 'ORDERS' ? 'active' : ''}`}
          onClick={() => setActiveTab('ORDERS')}
        >
          Đơn hàng
        </button>
        <button
          className={`notification-tab-btn ${activeTab === 'PAYMENTS' ? 'active' : ''}`}
          onClick={() => setActiveTab('PAYMENTS')}
        >
          Thanh toán
        </button>
      </div>

      {/* List */}
      <div className="notification-center-list">
        {filteredNotifications.length === 0 ? (
          <div className="dropdown-empty" style={{ padding: '60px 20px' }}>
            Không có thông báo nào trong mục này
          </div>
        ) : (
          filteredNotifications.map((notif) => (
            <div
              key={notif.id}
              className={`notification-item ${!notif.isRead ? 'unread' : ''}`}
              onClick={() => handleNotificationClick(notif)}
              style={{ borderRadius: '8px', marginBottom: '8px', border: '1px solid #f1f5f9' }}
            >
              <div className={`notification-icon-wrapper ${getIconClass(notif.type)}`}>
                {getIcon(notif.type)}
              </div>
              <div className="notification-content">
                <div className="notification-title-row">
                  <h4 className="notification-item-title">{notif.title}</h4>
                  <span className="notification-item-time">{formatTime(notif.createdAt)}</span>
                </div>
                <p className="notification-item-message">{notif.message}</p>
                {notif.isRead && notif.readAt && (
                  <span style={{ fontSize: '0.65rem', color: '#94a3b8', marginTop: '4px' }}>
                    Đã đọc lúc {new Date(notif.readAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                  </span>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Pagination Load More */}
      {hasMore && filteredNotifications.length > 0 && (
        <div className="load-more-container">
          <button
            className="load-more-btn"
            onClick={fetchMoreNotifications}
            disabled={loading}
          >
            {loading ? 'Đang tải...' : 'Tải thêm thông báo'}
          </button>
        </div>
      )}
    </div>
  );
}
