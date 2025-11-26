import { useEffect, useState } from 'react';
import { merchantService } from '../../services/api';
import OrderDetailModal from '../common/OrderDetailModal';

const MerchantOrders = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
    const [orderToUpdate, setOrderToUpdate] = useState(null);
    const [newStatus, setNewStatus] = useState('');
    const [updating, setUpdating] = useState(false);

    const ORDER_STATUSES = [
        { value: 'PENDING', label: 'Chờ xử lý', color: 'bg-yellow-100 text-yellow-800' },
        { value: 'CONFIRMED', label: 'Đã xác nhận', color: 'bg-blue-100 text-blue-800' },
        { value: 'PAID', label: 'Đã thanh toán', color: 'bg-green-100 text-green-800' },
        { value: 'SHIPPED', label: 'Đang giao hàng', color: 'bg-indigo-100 text-indigo-800' },
        { value: 'DELIVERED', label: 'Đã giao hàng', color: 'bg-green-100 text-green-800' },
        { value: 'CANCELLED', label: 'Đã hủy', color: 'bg-red-100 text-red-800' },
        { value: 'REFUNDED', label: 'Đã hoàn tiền', color: 'bg-purple-100 text-purple-800' },
    ];

    const fetchOrders = async () => {
        try {
            const response = await merchantService.getOrders();
            setOrders(response.data.content || []);
            setLoading(false);
        } catch (error) {
            console.error("Error fetching orders:", error);
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchOrders();
    }, []);

    const handleViewDetail = async (orderId) => {
        try {
            const response = await merchantService.getOrderDetail(orderId);
            setSelectedOrder(response.data);
            setIsModalOpen(true);
        } catch (error) {
            console.error("Error fetching order details:", error);
            alert("Failed to load order details");
        }
    };

    const closeModal = () => {
        setIsModalOpen(false);
        setSelectedOrder(null);
    };

    const handleOpenUpdateModal = (order) => {
        setOrderToUpdate(order);
        setNewStatus(order.status);
        setIsUpdateModalOpen(true);
    };

    const handleCloseUpdateModal = () => {
        setIsUpdateModalOpen(false);
        setOrderToUpdate(null);
        setNewStatus('');
    };

    const handleUpdateStatus = async () => {
        if (!orderToUpdate || !newStatus) {
            alert("Please select a status");
            return;
        }

        if (newStatus === orderToUpdate.status) {
            alert("Please select a different status");
            return;
        }

        setUpdating(true);
        try {
            await merchantService.updateOrderStatus(orderToUpdate.id, newStatus);
            alert(`Order ${orderToUpdate.orderCode} status updated to ${newStatus} successfully!`);
            handleCloseUpdateModal();
            // Refresh orders list
            await fetchOrders();
        } catch (error) {
            console.error("Error updating order status:", error);
            const errorMessage = error.response?.data?.message || error.message || "Failed to update order status";
            alert(`Error: ${errorMessage}`);
        } finally {
            setUpdating(false);
        }
    };

    const getStatusColor = (status) => {
        const statusObj = ORDER_STATUSES.find(s => s.value === status);
        return statusObj ? statusObj.color : 'bg-gray-100 text-gray-800';
    };

    const getStatusLabel = (status) => {
        const statusObj = ORDER_STATUSES.find(s => s.value === status);
        return statusObj ? statusObj.label : status;
    };

    if (loading) return <div>Loading orders...</div>;

    return (
        <div className="bg-white rounded-lg shadow-md overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200">
                <h3 className="text-lg font-semibold text-gray-800">Recent Orders</h3>
            </div>
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order Code</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {orders.map((order) => (
                            <tr key={order.id}>
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-blue-600">{order.orderCode}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {new Date(order.createdAt).toLocaleDateString()}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                    {(order.grandTotal || 0).toLocaleString()} VND
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(order.status)}`}>
                                        {getStatusLabel(order.status)}
                                    </span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    <button
                                        onClick={() => handleViewDetail(order.id)}
                                        className="text-indigo-600 hover:text-indigo-900 mr-3"
                                    >
                                        View
                                    </button>
                                    <button
                                        onClick={() => handleOpenUpdateModal(order)}
                                        className="text-green-600 hover:text-green-900"
                                    >
                                        Update
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Order Detail Modal */}
            {isModalOpen && (
                <OrderDetailModal
                    order={selectedOrder}
                    onClose={closeModal}
                />
            )}

            {/* Update Status Modal */}
            {isUpdateModalOpen && orderToUpdate && (
                <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
                    <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
                        <div className="mt-3">
                            <h3 className="text-lg leading-6 font-medium text-gray-900 mb-4">
                                Update Order Status
                            </h3>

                            <div className="mb-4">
                                <p className="text-sm text-gray-600 mb-2">
                                    Order: <span className="font-semibold text-gray-900">{orderToUpdate.orderCode}</span>
                                </p>
                                <p className="text-sm text-gray-600 mb-4">
                                    Current Status: <span className={`px-2 py-1 rounded-full text-xs font-semibold ${getStatusColor(orderToUpdate.status)}`}>
                                        {getStatusLabel(orderToUpdate.status)}
                                    </span>
                                </p>

                                <label htmlFor="status-select" className="block text-sm font-medium text-gray-700 mb-2">
                                    Select New Status
                                </label>
                                <select
                                    id="status-select"
                                    value={newStatus}
                                    onChange={(e) => setNewStatus(e.target.value)}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                                    disabled={updating}
                                >
                                    {ORDER_STATUSES.map((status) => (
                                        <option key={status.value} value={status.value}>
                                            {status.label}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="flex justify-end space-x-3">
                                <button
                                    onClick={handleCloseUpdateModal}
                                    className="px-4 py-2 bg-gray-300 text-gray-700 rounded-md hover:bg-gray-400 disabled:opacity-50"
                                    disabled={updating}
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={handleUpdateStatus}
                                    className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50"
                                    disabled={updating}
                                >
                                    {updating ? 'Updating...' : 'Update Status'}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default MerchantOrders;
