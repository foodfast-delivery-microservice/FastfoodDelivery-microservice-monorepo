import React from 'react';

const OrderHistoryTable = ({ orders, loading, onViewDetail }) => {
    const getStatusColor = (status) => {
        switch (status) {
            case 'COMPLETED': return 'bg-green-100 text-green-800 border border-green-200';
            case 'PENDING': return 'bg-yellow-100 text-yellow-800 border border-yellow-200';
            case 'DELIVERING': return 'bg-blue-100 text-blue-800 border border-blue-200';
            case 'CANCELLED': return 'bg-red-100 text-red-800 border border-red-200';
            default: return 'bg-gray-100 text-gray-800 border border-gray-200';
        }
    };

    if (loading) {
        return <div className="text-center py-4">Loading orders...</div>;
    }

    if (!orders || orders.length === 0) {
        return <div className="text-center text-gray-500 py-4">No orders found.</div>;
    }

    return (
        <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                    <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Order Code</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
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
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 text-right">
                                {order.grandTotal?.toLocaleString()} VND
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap">
                                <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(order.status)}`}>
                                    {order.status}
                                </span>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <button
                                    onClick={() => onViewDetail(order.id)}
                                    className="text-indigo-600 hover:text-indigo-900"
                                >
                                    View Detail
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default OrderHistoryTable;
