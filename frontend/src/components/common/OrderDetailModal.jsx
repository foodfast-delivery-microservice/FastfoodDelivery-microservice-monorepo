import React from 'react';

const OrderDetailModal = ({ order, onClose }) => {
    if (!order) return null;

    return (
        <div className="fixed inset-0 bg-gray-900 bg-opacity-50 backdrop-blur-sm overflow-y-auto h-full w-full flex items-center justify-center z-50">
            <div className="relative bg-white rounded-xl shadow-2xl max-w-3xl w-full mx-4 my-6 flex flex-col max-h-[90vh]">
                {/* Header */}
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
                    <div>
                        <h3 className="text-xl font-bold text-gray-900">
                            Order Details
                        </h3>
                        <p className="text-sm text-gray-500 mt-1">#{order.orderCode}</p>
                    </div>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-600 transition-colors p-2 rounded-full hover:bg-gray-100"
                    >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {/* Body */}
                <div className="p-6 overflow-y-auto custom-scrollbar">
                    {/* Status Bar */}
                    <div className="flex flex-wrap gap-4 justify-between items-center bg-gray-50 p-4 rounded-lg mb-6 border border-gray-100">
                        <div className="flex items-center gap-3">
                            <span className="text-sm font-medium text-gray-500">Status:</span>
                            <span className={`px-3 py-1 text-sm font-semibold rounded-full 
                                ${order.status === 'COMPLETED' ? 'bg-green-100 text-green-700 border border-green-200' :
                                    order.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700 border border-yellow-200' :
                                        order.status === 'CANCELLED' ? 'bg-red-100 text-red-700 border border-red-200' :
                                            'bg-blue-100 text-blue-700 border border-blue-200'}`}>
                                {order.status}
                            </span>
                        </div>
                        <div className="flex items-center gap-3">
                            <span className="text-sm font-medium text-gray-500">Order Date:</span>
                            <span className="text-sm font-medium text-gray-900">
                                {new Date(order.createdAt).toLocaleString()}
                            </span>
                        </div>
                    </div>

                    {/* Info Grid */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                        {/* Customer Info */}
                        <div className="bg-white p-4 rounded-lg border border-gray-100 shadow-sm">
                            <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
                                Customer
                            </h4>
                            <div className="space-y-2">
                                <p className="text-sm text-gray-600"><span className="font-medium text-gray-900">ID:</span> {order.userId}</p>
                            </div>
                        </div>

                        {/* Merchant Info */}
                        <div className="bg-white p-4 rounded-lg border border-gray-100 shadow-sm">
                            <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg>
                                Merchant
                            </h4>
                            <div className="space-y-2">
                                <p className="text-sm text-gray-600"><span className="font-medium text-gray-900">ID:</span> {order.merchantId}</p>
                            </div>
                        </div>

                        {/* Delivery Info */}
                        <div className="bg-white p-4 rounded-lg border border-gray-100 shadow-sm">
                            <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3 flex items-center gap-2">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                                Delivery To
                            </h4>
                            {order.deliveryAddress ? (
                                <div className="space-y-1">
                                    <p className="text-sm font-medium text-gray-900">{order.deliveryAddress.receiverName}</p>
                                    <p className="text-sm text-gray-500">{order.deliveryAddress.receiverPhone}</p>
                                    <p className="text-sm text-gray-600 mt-2 leading-relaxed">
                                        {order.deliveryAddress.addressLine1}, {order.deliveryAddress.ward}, {order.deliveryAddress.district}, {order.deliveryAddress.city}
                                    </p>
                                </div>
                            ) : (
                                <p className="text-sm text-gray-400 italic">No delivery info</p>
                            )}
                        </div>
                    </div>

                    {/* Order Items */}
                    <div className="mb-8">
                        <h4 className="text-sm font-bold text-gray-900 mb-4">Order Items</h4>
                        <div className="border border-gray-200 rounded-lg overflow-hidden">
                            <table className="min-w-full divide-y divide-gray-200">
                                <thead className="bg-gray-50">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Product</th>
                                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Price</th>
                                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Qty</th>
                                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Total</th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-200">
                                    {order.orderItems && order.orderItems.map((item, index) => (
                                        <tr key={index} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-6 py-4 text-sm text-gray-900 font-medium">
                                                {item.productName || `Product #${item.productId}`}
                                            </td>
                                            <td className="px-6 py-4 text-sm text-gray-500 text-right">
                                                {item.unitPrice?.toLocaleString()} VND
                                            </td>
                                            <td className="px-6 py-4 text-sm text-gray-500 text-right">
                                                {item.quantity}
                                            </td>
                                            <td className="px-6 py-4 text-sm text-gray-900 font-medium text-right">
                                                {(item.unitPrice * item.quantity).toLocaleString()} VND
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Summary */}
                    <div className="flex justify-end">
                        <div className="w-full sm:w-1/2 lg:w-1/3 space-y-3">
                            <div className="flex justify-between text-sm text-gray-600">
                                <span>Subtotal</span>
                                <span>{order.orderItems?.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0).toLocaleString()} VND</span>
                            </div>
                            <div className="flex justify-between text-sm text-gray-600">
                                <span>Shipping Fee</span>
                                <span>{order.shippingFee?.toLocaleString()} VND</span>
                            </div>
                            {order.discount > 0 && (
                                <div className="flex justify-between text-sm text-green-600">
                                    <span>Discount</span>
                                    <span>-{order.discount.toLocaleString()} VND</span>
                                </div>
                            )}
                            <div className="pt-3 border-t border-gray-200 mt-3">
                                <div className="flex justify-between items-center">
                                    <span className="text-base font-bold text-gray-900">Total Amount</span>
                                    <span className="text-xl font-bold text-blue-600">{order.grandTotal?.toLocaleString()} VND</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="bg-gray-50 px-6 py-4 border-t border-gray-200 rounded-b-xl flex justify-end">
                    <button
                        type="button"
                        className="px-6 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                        onClick={onClose}
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default OrderDetailModal;
