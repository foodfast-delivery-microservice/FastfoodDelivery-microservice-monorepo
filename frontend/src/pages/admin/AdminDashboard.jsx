import { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import UserList from '../../components/admin/UserList';
import MerchantList from '../../components/admin/MerchantList';
import SystemOrders from '../../components/admin/SystemOrders';
import AdminOverview from '../../components/admin/AdminOverview';

const AdminDashboard = () => {
    const { logout, user } = useAuth();
    const [activeTab, setActiveTab] = useState('dashboard');

    return (
        <div className="min-h-screen bg-gray-100">
            <nav className="bg-white shadow-sm">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16">
                        <div className="flex items-center">
                            <h1 className="text-xl font-bold text-gray-800">Admin Portal</h1>
                        </div>
                        <div className="flex items-center">
                            <span className="mr-4 text-gray-600">Welcome, {user?.username}</span>
                            <button
                                onClick={logout}
                                className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600 transition"
                            >
                                Logout
                            </button>
                        </div>
                    </div>
                </div>
            </nav>

            <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
                <div className="px-4 py-6 sm:px-0">
                    {/* Tabs */}
                    <div className="mb-6 border-b border-gray-200">
                        <nav className="-mb-px flex space-x-8">
                            <button
                                onClick={() => setActiveTab('dashboard')}
                                className={`${activeTab === 'dashboard'
                                    ? 'border-blue-500 text-blue-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                    } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-150`}
                            >
                                📊 Dashboard
                            </button>
                            <button
                                onClick={() => setActiveTab('users')}
                                className={`${activeTab === 'users'
                                    ? 'border-blue-500 text-blue-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                    } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-150`}
                            >
                                Users
                            </button>
                            <button
                                onClick={() => setActiveTab('merchants')}
                                className={`${activeTab === 'merchants'
                                    ? 'border-blue-500 text-blue-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                    } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-150`}
                            >
                                Merchants
                            </button>
                            <button
                                onClick={() => setActiveTab('orders')}
                                className={`${activeTab === 'orders'
                                    ? 'border-blue-500 text-blue-600'
                                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                    } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-150`}
                            >
                                System Orders
                            </button>
                        </nav>
                    </div>

                    {/* Content */}
                    <div className="mt-6">
                        {activeTab === 'dashboard' && <AdminOverview />}
                        {activeTab === 'users' && <UserList />}
                        {activeTab === 'merchants' && <MerchantList />}
                        {activeTab === 'orders' && <SystemOrders />}
                    </div>
                </div>
            </main>
        </div>
    );
};

export default AdminDashboard;
