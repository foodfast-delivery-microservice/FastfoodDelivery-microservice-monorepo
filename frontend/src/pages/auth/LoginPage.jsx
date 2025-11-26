import { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { authService } from '../../services/api';

const LoginPage = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            const response = await authService.login({ username, password });
            const data = response.data.data; // ApiResponse wrapper: { status, message, data: { ... } }

            login(data);

            // Decode to check role for redirection (or rely on what login() set in context, but context update might be async)
            // We can re-decode here or just let the user be redirected by a useEffect or just check the token again
            // For simplicity, let's re-decode or check the data if we trust AuthContext logic

            // Let's decode locally for immediate redirection decision
            const token = data.accessToken;
            const payload = JSON.parse(atob(token.split('.')[1]));
            const role = payload.role || 'USER';

            if (role === 'ADMIN') navigate('/admin');
            else if (role === 'MERCHANT') navigate('/merchant');
            else navigate('/');

        } catch (err) {
            console.error("Login failed", err);
            setError('Login failed. Please check your credentials.');
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
            <div className="bg-white p-8 rounded shadow-md w-96">
                <h2 className="text-2xl font-bold mb-6 text-center">Login</h2>
                {error && <div className="bg-red-100 text-red-700 p-2 mb-4 rounded">{error}</div>}
                <form onSubmit={handleSubmit}>
                    <div className="mb-4">
                        <label className="block text-gray-700 mb-2">Username</label>
                        <input
                            type="text"
                            className="w-full p-2 border rounded"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                        />
                    </div>
                    <div className="mb-6">
                        <label className="block text-gray-700 mb-2">Password</label>
                        <input
                            type="password"
                            className="w-full p-2 border rounded"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                        />
                    </div>
                    <button
                        type="submit"
                        className="w-full bg-blue-600 text-white p-2 rounded hover:bg-blue-700"
                    >
                        Login
                    </button>
                </form>
                <div className="mt-4 text-sm text-gray-600">
                    <p>Hint: Use your registered credentials.</p>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;
