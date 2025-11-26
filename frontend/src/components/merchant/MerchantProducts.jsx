import { useEffect, useState } from 'react';
import { merchantService } from '../../services/api';
import ProductFormModal from './ProductFormModal';

const MerchantProducts = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState(null);

    useEffect(() => {
        fetchProducts();
    }, []);

    const fetchProducts = async () => {
        try {
            const response = await merchantService.getProducts();
            setProducts(response.data.data || []);
            setLoading(false);
        } catch (error) {
            console.error("Error fetching products:", error);
            setLoading(false);
        }
    };

    const handleAddProduct = () => {
        setSelectedProduct(null);
        setIsModalOpen(true);
    };

    const handleEditProduct = (product) => {
        setSelectedProduct(product);
        setIsModalOpen(true);
    };

    const handleDeleteProduct = async (product) => {
        // STRICT VALIDATION: Only allow delete if product is inactive
        if (product.active) {
            alert(
                `❌ KHÔNG THỂ XÓA PRODUCT ĐANG HOẠT ĐỘNG!\n\n` +
                `Sản phẩm "${product.name}" đang ở trạng thái HOẠT ĐỘNG.\n\n` +
                `Để xóa sản phẩm này, bạn cần:\n` +
                `1. Click nút "Edit"\n` +
                `2. Bỏ tick "Active (available for sale)"\n` +
                `3. Save để set product về trạng thái Inactive\n` +
                `4. Sau đó mới có thể xóa\n\n` +
                `💡 Lý do: Tránh xóa nhầm sản phẩm đang bán, ảnh hưởng khách hàng.`
            );
            return; // Block deletion
        }

        // Build confirmation message for inactive products
        let warningMessage = `Xóa sản phẩm "${product.name}"?\n\n`;

        if (product.stock > 0) {
            warningMessage += `⚠️ Còn ${product.stock} sản phẩm trong kho\n\n`;
        }

        warningMessage += 'Thông tin sản phẩm:\n';
        warningMessage += `- Giá: ${(product.price || 0).toLocaleString()} VND\n`;
        warningMessage += `- Loại: ${product.category}\n`;
        warningMessage += `- Trạng thái: Ngưng bán\n\n`;
        warningMessage += '⚠️ Hành động này KHÔNG THỂ HOÀN TÁC!';

        if (!window.confirm(warningMessage)) return;

        try {
            await merchantService.deleteProduct(product.id);
            fetchProducts(); // Refresh list
            alert(`✅ Đã xóa sản phẩm "${product.name}" thành công`);
        } catch (error) {
            console.error("Error deleting product:", error);
            const errorMsg = error.response?.data?.message || error.message || 'Không xác định';
            alert(`❌ Không thể xóa sản phẩm: ${errorMsg}\n\nLưu ý: Không thể xóa sản phẩm đang có trong đơn hàng chưa hoàn thành.`);
        }
    };

    const handleSaveProduct = async (productData) => {
        try {
            if (selectedProduct) {
                // Update existing product
                await merchantService.updateProduct(selectedProduct.id, productData);
                alert('Product updated successfully');
            } else {
                // Create new product
                await merchantService.createProduct(productData);
                alert('Product created successfully');
            }
            fetchProducts(); // Refresh list
        } catch (error) {
            console.error("Error saving product:", error);
            throw error; // Will be caught by modal
        }
    };

    if (loading) return <div>Loading products...</div>;

    return (
        <div className="bg-white rounded-lg shadow-md overflow-hidden mt-8">
            <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
                <h3 className="text-lg font-semibold text-gray-800">My Products</h3>
                <button
                    onClick={handleAddProduct}
                    className="bg-blue-600 text-white px-4 py-2 rounded text-sm hover:bg-blue-700 transition-colors"
                >
                    Add Product
                </button>
            </div>
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Category</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Price</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {products.map((product) => (
                            <tr key={product.id} className="hover:bg-gray-50 transition-colors">
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{product.name}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{product.category}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{(product.price || 0).toLocaleString()} VND</td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${product.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                                        }`}>
                                        {product.active ? 'Hoạt động' : 'Không hoạt động'}
                                    </span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 space-x-3">
                                    <button
                                        onClick={() => handleEditProduct(product)}
                                        className="text-blue-600 hover:text-blue-900 font-medium transition-colors"
                                    >
                                        Edit
                                    </button>
                                    <button
                                        onClick={() => handleDeleteProduct(product)}
                                        className="text-red-600 hover:text-red-900 font-medium transition-colors"
                                    >
                                        Delete
                                    </button>
                                </td>
                            </tr>
                        ))}
                        {products.length === 0 && (
                            <tr>
                                <td colSpan="5" className="px-6 py-10 text-center text-gray-500">
                                    No products found. Click "Add Product" to get started.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {isModalOpen && (
                <ProductFormModal
                    product={selectedProduct}
                    onClose={() => setIsModalOpen(false)}
                    onSave={handleSaveProduct}
                />
            )}
        </div>
    );
};

export default MerchantProducts;
