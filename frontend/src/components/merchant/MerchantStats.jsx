import { useEffect, useState } from 'react';
import { merchantService } from '../../services/api';
import { DollarSign, ShoppingBag, TrendingUp } from 'lucide-react';

const MerchantStats = () => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const response = await merchantService.getStatistics();
                setStats(response.data);
                setLoading(false);
            } catch (error) {
                console.error("Error fetching stats:", error);
                setLoading(false);
            }
        };

        fetchStats();
    }, []);

    if (loading) return <div>Loading stats...</div>;

    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <div className="bg-white p-6 rounded-lg shadow-md flex items-center">
                <div className="p-3 rounded-full bg-green-100 text-green-600 mr-4">
                    <DollarSign size={24} />
                </div>
                <div>
                    <p className="text-gray-500 text-sm">Total Revenue</p>
                    <p className="text-2xl font-bold">{stats?.totalRevenue.toLocaleString()} VND</p>
                </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow-md flex items-center">
                <div className="p-3 rounded-full bg-blue-100 text-blue-600 mr-4">
                    <ShoppingBag size={24} />
                </div>
                <div>
                    <p className="text-gray-500 text-sm">Total Orders</p>
                    <p className="text-2xl font-bold">{stats?.totalOrders}</p>
                </div>
            </div>

            <div className="bg-white p-6 rounded-lg shadow-md flex items-center">
                <div className="p-3 rounded-full bg-purple-100 text-purple-600 mr-4">
                    <TrendingUp size={24} />
                </div>
                <div>
                    <p className="text-gray-500 text-sm">Avg. Order Value</p>
                    <p className="text-2xl font-bold">
                        {stats?.totalOrders > 0
                            ? (stats.totalRevenue / stats.totalOrders).toLocaleString()
                            : '0'} VND
                    </p>
                </div>
            </div>
        </div>
    );
};

export default MerchantStats;
