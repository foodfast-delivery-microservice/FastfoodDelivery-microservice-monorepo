import { useState, useEffect } from 'react';
import { adminService } from '../../services/api';

const AdminOverview = () => {
    const [kpis, setKpis] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchKPIs();
        // Auto-refresh every 60 seconds
        const interval = setInterval(fetchKPIs, 60000);
        return () => clearInterval(interval);
    }, []);

    const fetchKPIs = async () => {
        try {
            const response = await adminService.getSystemKPIs();
            setKpis(response.data);
            setError(null);
        } catch (err) {
            console.error('Error fetching KPIs:', err);
            setError('Failed to load KPIs');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
                {error}
            </div>
        );
    }

    if (!kpis) return null;

    const statusColors = {
        PENDING: 'bg-yellow-100 text-yellow-800',
        CONFIRMED: 'bg-blue-100 text-blue-800',
        PAID: 'bg-green-100 text-green-800',
        SHIPPED: 'bg-purple-100 text-purple-800',
        DELIVERED: 'bg-green-100 text-green-800',
        CANCELLED: 'bg-red-100 text-red-800',
        REFUNDED: 'bg-orange-100 text-orange-800',
    };

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold text-gray-900">System Overview</h2>
                <div className="text-sm text-gray-500">
                    Auto-refreshes every 60s • Last updated: {new Date().toLocaleTimeString()}
                </div>
            </div>

            {/* KPI Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {/* Total Orders Today */}
                <div className="bg-white p-6 rounded-lg shadow-md border-l-4 border-blue-500">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm font-medium text-gray-600 uppercase">Total Orders Today</p>
                            <p className="text-3xl font-bold text-gray-900 mt-2">
                                {kpis.todayTotalOrders || 0}
                            </p>
                        </div>
                        <div className="text-blue-500">
                            <svg className="w-12 h-12" fill="currentColor" viewBox="0 0 20 20">
                                <path d="M3 1a1 1 0 000 2h1.22l.305 1.222a.997.997 0 00.01.042l1.358 5.43-.893.892C3.74 11.846 4.632 14 6.414 14H15a1 1 0 000-2H6.414l1-1H14a1 1 0 00.894-.553l3-6A1 1 0 0017 3H6.28l-.31-1.243A1 1 0 005 1H3zM16 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM6.5 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3z" />
                            </svg>
                        </div>
                    </div>
                </div>

                {/* Pending Orders Alert */}
                <div className={`bg-white p-6 rounded-lg shadow-md border-l-4 ${kpis.pendingOrdersCount > 10 ? 'border-red-500' : 'border-yellow-500'
                    }`}>
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm font-medium text-gray-600 uppercase">Pending Orders</p>
                            <p className="text-3xl font-bold text-gray-900 mt-2">
                                {kpis.pendingOrdersCount || 0}
                            </p>
                            {kpis.pendingOrdersCount > 10 && (
                                <p className="text-xs text-red-600 mt-1">⚠️ High volume!</p>
                            )}
                        </div>
                        <div className={kpis.pendingOrdersCount > 10 ? 'text-red-500' : 'text-yellow-500'}>
                            <svg className="w-12 h-12" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                            </svg>
                        </div>
                    </div>
                </div>

                {/* Recent Orders (Last 10 min) */}
                <div className="bg-white p-6 rounded-lg shadow-md border-l-4 border-green-500">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm font-medium text-gray-600 uppercase">Recent Orders</p>
                            <p className="text-xs text-gray-500">(Last 10 minutes)</p>
                            <p className="text-3xl font-bold text-gray-900 mt-2">
                                {kpis.recentOrdersCount || 0}
                            </p>
                        </div>
                        <div className="text-green-500">
                            <svg className="w-12 h-12" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd" />
                            </svg>
                        </div>
                    </div>
                </div>

                {/* Average Order Value */}
                <div className="bg-white p-6 rounded-lg shadow-md border-l-4 border-purple-500">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm font-medium text-gray-600 uppercase">Avg Order Value</p>
                            <p className="text-3xl font-bold text-gray-900 mt-2">
                                {(kpis.averageOrderValue || 0).toLocaleString()} đ
                            </p>
                        </div>
                        <div className="text-purple-500">
                            <svg className="w-12 h-12" fill="currentColor" viewBox="0 0 20 20">
                                <path d="M8.433 7.418c.155-.103.346-.196.567-.267v1.698a2.305 2.305 0 01-.567-.267C8.07 8.34 8 8.114 8 8c0-.114.07-.34.433-.582zM11 12.849v-1.698c.22.071.412.164.567.267.364.243.433.468.433.582 0 .114-.07.34-.433.582a2.305 2.305 0 01-.567.267z" />
                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-13a1 1 0 10-2 0v.092a4.535 4.535 0 00-1.676.662C6.602 6.234 6 7.009 6 8c0 .99.602 1.765 1.324 2.246.48.32 1.054.545 1.676.662v1.941c-.391-.127-.68-.317-.843-.504a1 1 0 10-1.51 1.31c.562.649 1.413 1.076 2.353 1.253V15a1 1 0 102 0v-.092a4.535 4.535 0 001.676-.662C13.398 13.766 14 12.991 14 12c0-.99-.602-1.765-1.324-2.246A4.535 4.535 0 0011 9.092V7.151c.391.127.68.317.843.504a1 1 0 101.511-1.31c-.563-.649-1.413-1.076-2.354-1.253V5z" clipRule="evenodd" />
                            </svg>
                        </div>
                    </div>
                </div>
            </div>

            {/* Orders by Status */}
            <div className="bg-white p-6 rounded-lg shadow-md">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Today's Orders by Status</h3>
                <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
                    {Object.entries(kpis.todayOrdersByStatus || {}).map(([status, count]) => (
                        <div
                            key={status}
                            className={`p-4 rounded-lg ${statusColors[status] || 'bg-gray-100 text-gray-800'} text-center`}
                        >
                            <div className="text-2xl font-bold">{count}</div>
                            <div className="text-xs font-medium mt-1">{status}</div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default AdminOverview;
