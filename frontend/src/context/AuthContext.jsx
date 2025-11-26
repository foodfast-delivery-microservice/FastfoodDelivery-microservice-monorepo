import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check for stored token/user on mount
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
            setUser(JSON.parse(storedUser));
        }
        setLoading(false);
    }, []);

    const login = (data) => {
        // data can be just the token response or an object with token
        const token = data.accessToken || data.token;
        const userData = { ...data, token };

        // Decode token to get role
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            userData.role = payload.role || payload.scope || 'USER'; // Fallback
            // Handle scope if it's a list
            if (Array.isArray(userData.role)) {
                if (userData.role.includes('ADMIN')) userData.role = 'ADMIN';
                else if (userData.role.includes('MERCHANT')) userData.role = 'MERCHANT';
                else userData.role = 'USER';
            }
        } catch (e) {
            console.error("Failed to decode token", e);
            userData.role = 'USER';
        }

        setUser(userData);
        localStorage.setItem('user', JSON.stringify(userData));
    };

    const logout = () => {
        setUser(null);
        localStorage.removeItem('user');
        localStorage.removeItem('token');
    };

    const value = {
        user,
        login,
        logout,
        loading
    };

    return (
        <AuthContext.Provider value={value}>
            {!loading && children}
        </AuthContext.Provider>
    );
};
