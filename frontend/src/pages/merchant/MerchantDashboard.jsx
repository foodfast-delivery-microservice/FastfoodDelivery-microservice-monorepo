import { useAuth } from '../../context/AuthContext';
import MerchantStats from '../../components/merchant/MerchantStats';
import MerchantOrders from '../../components/merchant/MerchantOrders';
import MerchantProducts from '../../components/merchant/MerchantProducts';

const MerchantDashboard = () => {
    const { logout, user } = useAuth();

    return (
        <div className="min-h-screen bg-gray-100">
            <nav className="bg-white shadow-sm">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16">
                        <div className="flex items-center">
                            <h1 className="text-xl font-bold text-gray-800">Merchant Portal</h1>
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
                    <MerchantStats />
                    <MerchantOrders />
                    <MerchantProducts />
                </div>
            </main>
        </div>
    );
};

export default MerchantDashboard;
