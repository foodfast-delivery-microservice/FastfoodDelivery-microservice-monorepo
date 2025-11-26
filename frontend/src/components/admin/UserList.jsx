import { useEffect, useState } from 'react';
import { adminService } from '../../services/api';
import UserOrderHistoryModal from './UserOrderHistoryModal';

const UserList = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedUserId, setSelectedUserId] = useState(null);
    const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false);

    useEffect(() => {
        const fetchUsers = async () => {
            try {
                const response = await adminService.getUsers();
                const allUsers = response.data.data; // ApiResponse wrapper: { status, message, data: [...] }

                // Filter out merchants
                const userList = allUsers.filter(user => user.role !== 'MERCHANT');

                // Enrich with stats
                const enrichedUsers = await Promise.all(userList.map(async (user) => {
                    try {
                        const statsResponse = await adminService.getUserStatistics(user.id);
                        if (statsResponse) {
                            return { ...user, stats: statsResponse.data };
                        }
                    } catch (err) {
                        console.error(`Failed to fetch stats for user ${user.id}`, err);
                    }
                    return user;
                }));

                setUsers(enrichedUsers);
                setLoading(false);
            } catch (error) {
                console.error("Error fetching users:", error);
                setLoading(false);
            }
        };

        fetchUsers();
    }, []);

    const handleStatusChange = async (userId, newStatus) => {
        try {
            await adminService.updateUserStatus(userId, newStatus);
            // Update local state
            setUsers(users.map(user =>
                user.id === userId ? { ...user, active: newStatus } : user
            ));
        } catch (error) {
            console.error("Error updating user status:", error);
            alert("Failed to update user status");
        }
    };

    const handleViewOrders = (userId) => {
        setSelectedUserId(userId);
        setIsHistoryModalOpen(true);
    };

    const closeHistoryModal = () => {
        setIsHistoryModalOpen(false);
        setSelectedUserId(null);
    };

    if (loading) return <div className="text-center py-4">Loading users...</div>;

    return (
        <div className="bg-white shadow rounded-lg overflow-hidden">
            <div className="px-4 py-5 sm:px-6">
                <h3 className="text-lg leading-6 font-medium text-gray-900">Users</h3>
            </div>
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Username</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Spent</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Orders</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {users.map((user) => (
                            <tr key={user.id}>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">{user.id}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{user.username}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{user.email}</td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${user.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                                        }`}>
                                        {user.active ? 'Active' : 'Blocked'}
                                    </span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                                    {user.stats ? `${user.stats.totalSpent?.toLocaleString() || 0} VND` : '-'}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-right">
                                    {user.stats ? user.stats.totalOrders : '-'}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                                    <button
                                        onClick={() => handleViewOrders(user.id)}
                                        className="text-blue-600 hover:text-blue-900"
                                    >
                                        View Orders
                                    </button>
                                    {user.active ? (
                                        <button
                                            onClick={() => handleStatusChange(user.id, false)}
                                            className="text-red-600 hover:text-red-900"
                                        >
                                            Block
                                        </button>
                                    ) : (
                                        <button
                                            onClick={() => handleStatusChange(user.id, true)}
                                            className="text-green-600 hover:text-green-900"
                                        >
                                            Unblock
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {isHistoryModalOpen && (
                <UserOrderHistoryModal
                    userId={selectedUserId}
                    role={users.find(u => u.id === selectedUserId)?.role}
                    onClose={closeHistoryModal}
                />
            )}
        </div>
    );
};

export default UserList;
