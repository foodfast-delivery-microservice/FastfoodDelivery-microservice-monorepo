import { useState, useEffect } from 'react';
import { adminService } from '../../services/api';
import MerchantDetailModal from './MerchantDetailModal';

const MerchantList = () => {
    const [merchants, setMerchants] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedMerchant, setSelectedMerchant] = useState(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);

    useEffect(() => {
        fetchMerchants();
    }, []);

    const fetchMerchants = async () => {
        try {
            const response = await adminService.getUsers();
            const allUsers = response.data.data || [];
            // Filter only merchants
            const merchantList = allUsers.filter(user => user.role === 'MERCHANT');

            // Fetch stats for each merchant
            const merchantsWithStats = await Promise.all(merchantList.map(async (merchant) => {
                try {
                    const statsResponse = await adminService.getMerchantStats(merchant.id);
                    return { ...merchant, stats: statsResponse.data };
                } catch (error) {
                    console.error(`Error fetching stats for merchant ${merchant.id}:`, error);
                    return { ...merchant, stats: null };
                }
            }));

            setMerchants(merchantsWithStats);
            setLoading(false);
        } catch (error) {
            console.error("Error fetching merchants:", error);
            setLoading(false);
        }
    };

    const handleViewDetails = (merchant) => {
        setSelectedMerchant(merchant);
        setIsDetailModalOpen(true);
    };

    const closeDetailModal = () => {
        setIsDetailModalOpen(false);
        setSelectedMerchant(null);
    };

    const handleBlockMerchant = async (merchantId, currentStatus) => {
        if (!window.confirm(`Are you sure you want to ${currentStatus ? 'block' : 'unblock'} this merchant?`)) return;

        try {
            await adminService.updateUserStatus(merchantId, !currentStatus);
            fetchMerchants(); // Refresh list
        } catch (error) {
            console.error("Error updating merchant status:", error);
            alert("Failed to update merchant status");
        }
    };

    if (loading) return <div className="text-center py-10">Loading merchants...</div>;

    return (
        <div className="bg-white shadow rounded-lg overflow-hidden">
            <div className="px-4 py-5 sm:px-6 flex justify-between items-center border-b border-gray-200">
                <h3 className="text-lg leading-6 font-medium text-gray-900">Merchant Management</h3>
                <span className="bg-blue-100 text-blue-800 text-xs font-semibold px-2.5 py-0.5 rounded dark:bg-blue-200 dark:text-blue-800">
                    Total: {merchants.length}
                </span>
            </div>

            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Merchant Name</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Revenue</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Orders</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {merchants.map((merchant) => (
                            <tr key={merchant.id} className="hover:bg-gray-50 transition-colors">
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">{merchant.id}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{merchant.username}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{merchant.email}</td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${merchant.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                                        }`}>
                                        {merchant.active ? 'Active' : 'Blocked'}
                                    </span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 text-right font-medium">
                                    {merchant.stats?.totalRevenue?.toLocaleString() || 0} VND
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 text-right">
                                    {merchant.stats?.totalOrders || 0}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-3">
                                    <button
                                        onClick={() => handleViewDetails(merchant)}
                                        className="text-blue-600 hover:text-blue-900 font-medium"
                                    >
                                        View Details
                                    </button>
                                    <button
                                        onClick={() => handleBlockMerchant(merchant.id, merchant.active)}
                                        className={`${merchant.active ? 'text-red-600 hover:text-red-900' : 'text-green-600 hover:text-green-900'
                                            } font-medium`}
                                    >
                                        {merchant.active ? 'Block' : 'Unblock'}
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {merchants.length === 0 && (
                            <tr>
                                <td colSpan="7" className="px-6 py-10 text-center text-gray-500">
                                    No merchants found.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {isDetailModalOpen && selectedMerchant && (
                <MerchantDetailModal
                    merchant={selectedMerchant}
                    onClose={closeDetailModal}
                />
            )}
        </div>
    );
};

export default MerchantList;
