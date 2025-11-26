import { useState, useEffect } from 'react';
import { adminService } from '../../services/api';
import OrderHistoryTable from './OrderHistoryTable';
import OrderDetailModal from '../common/OrderDetailModal';

const MerchantDetailModal = ({ merchant, onClose }) => {
    const [activeTab, setActiveTab] = useState('products');
    const [products, setProducts] = useState([]);
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(false);
    const [stats, setStats] = useState(null);
    const [selectedOrder, setSelectedOrder] = useState(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

    useEffect(() => {
        if (merchant) {
            fetchStats();
            if (activeTab === 'products') {
                fetchProducts();
            } else {
                fetchOrders();
            }
        }
    }, [merchant, activeTab]);

    const fetchStats = async () => {
        try {
            const response = await adminService.getMerchantStats(merchant.id);
            setStats(response.data);
        } catch (error) {
            console.error("Error fetching merchant stats:", error);
        }
    };

    const fetchProducts = async () => {
        setLoading(true);
        try {
            const response = await adminService.getMerchantProducts(merchant.id, {});
            setProducts(response.data.data || []);
        } catch (error) {
            console.error("Error fetching products:", error);
        } finally {
            setLoading(false);
        }
    };

    const fetchOrders = async () => {
        setLoading(true);
        try {
            const response = await adminService.getAllOrders({ merchantId: merchant.id });
            setOrders(response.data.content || []);
        } catch (error) {
            console.error("Error fetching orders:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleViewOrderDetail = async (orderId) => {
        try {
            const response = await adminService.getOrderDetails(orderId);
            setSelectedOrder(response.data);
            setIsDetailModalOpen(true);
        } catch (error) {
            console.error("Error fetching order details:", error);
            alert("Failed to load order details");
        }
    };

    return (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full flex items-center justify-center z-40">
            <div className="relative bg-white rounded-lg shadow-xl max-w-6xl w-full mx-4 my-6 flex flex-col max-h-[90vh]">
                {/* Header */}
                <div className="flex items-center justify-between p-6 border-b bg-gray-50 rounded-t-lg">
                    <div>
                        <h3 className="text-2xl font-bold text-gray-900">{merchant.username}</h3>
                        <p className="text-sm text-gray-500 mt-1">ID: {merchant.id} • {merchant.email}</p>
                    </div>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-500 focus:outline-none">
                        <span className="text-3xl">&times;</span>
                    </button>
                </div>

                {/* Stats */}
                <div className="grid grid-cols-3 gap-6 p-6 bg-white border-b">
                    <div className="bg-blue-50 p-4 rounded-lg border border-blue-100">
                        <p className="text-sm font-medium text-blue-600 uppercase">Total Revenue</p>
                        <p className="text-2xl font-bold text-blue-900 mt-1">
                            {stats?.totalRevenue?.toLocaleString() || 0} VND
                        </p>
                    </div>
                    <div className="bg-green-50 p-4 rounded-lg border border-green-100">
                        <p className="text-sm font-medium text-green-600 uppercase">Total Orders</p>
                        <p className="text-2xl font-bold text-green-900 mt-1">
                            {stats?.totalOrders || 0}
                        </p>
                    </div>
                    <div className="bg-purple-50 p-4 rounded-lg border border-purple-100">
                        <p className="text-sm font-medium text-purple-600 uppercase">Total Products</p>
                        <p className="text-2xl font-bold text-purple-900 mt-1">
                            {products.length}
                        </p>
                    </div>
                </div>

                {/* Tabs */}
                <div className="border-b border-gray-200 px-6">
                    <nav className="-mb-px flex space-x-8">
                        <button
                            onClick={() => setActiveTab('products')}
                            className={`${activeTab === 'products'
                                    ? 'border-blue-500 text-blue-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-150`}
                        >
                            Products
                        </button>
                        <button
                            onClick={() => setActiveTab('orders')}
                            className={`${activeTab === 'orders'
                                    ? 'border-blue-500 text-blue-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-150`}
                        >
                            Orders History
                        </button>
                    </nav>
                </div>

                {/* Content */}
                <div className="p-6 overflow-y-auto flex-1 bg-gray-50">
                    {loading ? (
                        <div className="flex justify-center items-center h-40">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                        </div>
                    ) : activeTab === 'products' ? (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {products.map((product) => (
                                <div key={product.id} className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow duration-200">
                                    <div className="aspect-w-16 aspect-h-9 bg-gray-200 h-48">
                                        {product.imageUrl ? (
                                            <img
                                                src={product.imageUrl}
                                                alt={product.name}
                                                className="w-full h-full object-cover"
                                            />
                                        ) : (
                                            <div className="w-full h-full flex items-center justify-center text-gray-400">
                                                <span className="text-sm">No Image</span>
                                            </div>
                                        )}
                                    </div>
                                    <div className="p-4">
                                        <div className="flex justify-between items-start">
                                            <h4 className="text-lg font-semibold text-gray-900 line-clamp-1" title={product.name}>
                                                {product.name}
                                            </h4>
                                            <span className={`px-2 py-1 text-xs font-semibold rounded-full ${product.active
                                                    ? 'bg-green-100 text-green-800'
                                                    : 'bg-red-100 text-red-800'
                                                }`}>
                                                {product.active ? 'Active' : 'Inactive'}
                                            </span>
                                        </div>
                                        <p className="text-sm text-gray-500 mt-1 line-clamp-2 h-10">{product.description}</p>
                                        <div className="mt-4 flex justify-between items-center">
                                            <span className="text-lg font-bold text-blue-600">
                                                {(product.price || 0).toLocaleString()} VND
                                            </span>
                                            <span className="text-xs text-gray-400">ID: {product.id}</span>
                                        </div>
                                    </div>
                                </div>
                            ))}
                            {products.length === 0 && (
                                <div className="col-span-full text-center py-10 text-gray-500">
                                    No products found.
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
                            <OrderHistoryTable
                                orders={orders}
                                loading={loading}
                                onViewDetail={handleViewOrderDetail}
                            />
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="bg-gray-50 px-6 py-4 rounded-b-lg border-t flex justify-end">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 bg-white border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
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
                            onClose={() => setIsDetailModalOpen(false)}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default MerchantDetailModal;
