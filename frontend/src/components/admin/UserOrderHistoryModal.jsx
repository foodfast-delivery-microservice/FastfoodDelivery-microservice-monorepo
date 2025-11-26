import { useEffect, useState } from 'react';
import { adminService } from '../../services/api';
import OrderDetailModal from '../common/OrderDetailModal';
import OrderHistoryTable from './OrderHistoryTable';

const UserOrderHistoryModal = ({ userId, role, onClose }) => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

    useEffect(() => {
        const fetchUserOrders = async () => {
            try {
                const params = role === 'MERCHANT' ? { merchantId: userId } : { userId };
                const response = await adminService.getAllOrders(params);
                setOrders(response.data.content || []);
                setLoading(false);
            } catch (error) {
                console.error("Error fetching user orders:", error);
                setLoading(false);
            }
        };

        if (userId) {
            fetchUserOrders();
        }
    }, [userId]);

    const handleViewDetail = async (orderId) => {
        try {
            const response = await adminService.getOrderDetails(orderId);
            setSelectedOrder(response.data);
            setIsDetailModalOpen(true);
        } catch (error) {
            console.error("Error fetching order details:", error);
            alert("Failed to load order details");
        }
    };

    const closeDetailModal = () => {
        setIsDetailModalOpen(false);
        setSelectedOrder(null);
    };



    return (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full flex items-center justify-center z-40">
            <div className="relative bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 my-6 flex flex-col max-h-[90vh]">
                {/* Header */}
                <div className="flex items-center justify-between p-4 border-b">
                    <h3 className="text-xl font-semibold text-gray-900">
                        Order History (User #{userId})
                    </h3>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-500 focus:outline-none"
                    >
                        <span className="text-2xl">&times;</span>
                    </button>
                </div>

                {/* Body */}
                <div className="p-6 overflow-y-auto flex-1">
                    <OrderHistoryTable
                        orders={orders}
                        loading={loading}
                        onViewDetail={handleViewDetail}
                    />
                </div>

                {/* Footer */}
                <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse rounded-b-lg">
                    <button
                        type="button"
                        className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-gray-600 text-base font-medium text-white hover:bg-gray-700 focus:outline-none sm:ml-3 sm:w-auto sm:text-sm"
                        onClick={onClose}
                    >
                        Close
                    </button>
                </div>
            </div>

            {/* Nested Modal for Order Details */}
            {isDetailModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center">
                    <div className="absolute inset-0 bg-black opacity-50"></div>
                    <div className="relative z-50 w-full max-w-2xl">
                        <OrderDetailModal
                            order={selectedOrder}
                            onClose={closeDetailModal}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default UserOrderHistoryModal;
