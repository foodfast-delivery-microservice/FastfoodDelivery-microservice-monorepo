import { Link } from 'react-router-dom';

const UnauthorizedPage = () => {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100">
            <h1 className="text-4xl font-bold text-red-600 mb-4">403 - Unauthorized</h1>
            <p className="text-gray-700 mb-6">You do not have permission to access this page.</p>
            <Link to="/login" className="text-blue-600 hover:underline">
                Go to Login
            </Link>
        </div>
    );
};

export default UnauthorizedPage;
