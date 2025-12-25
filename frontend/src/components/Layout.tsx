import { Link, useLocation } from 'react-router-dom';

interface LayoutProps {
    children: React.ReactNode;
    wide?: boolean;
}

export function Layout({ children, wide = false }: LayoutProps) {
    const location = useLocation();

    const navItems = [
        { path: '/', label: 'Dashboard', id: 'dashboard' },
        { path: '/dependencies', label: 'Dependencies', id: 'dependencies' },
        { path: '/export', label: 'Export', id: 'export' },
    ];

    const isActive = (path: string) => {
        if (path === '/') return location.pathname === '/';
        return location.pathname.startsWith(path);
    };

    return (
        <div className="bg-background text-text-primary min-h-screen flex flex-col font-sans">
            {/* Top App Bar */}
            <header className="bg-surface sticky top-0 z-50 shadow-mate-1 border-b border-gray-100">
                <div className="px-6 h-16 flex items-center justify-between max-w-7xl mx-auto w-full">
                    <div className="flex items-center gap-4">
                        <Link
                            to="/"
                            className="flex items-center gap-2 text-primary-600 hover:text-primary-700 transition-colors"
                        >
                            <span className="material-symbols-outlined text-3xl">hub</span>
                            <h1 className="text-xl font-bold tracking-tight text-gray-900">
                                J-Dep Analyzer
                            </h1>
                        </Link>
                    </div>

                    <nav className="hidden md:flex gap-2 items-center">
                        {navItems.map((item) => (
                            <Link
                                key={item.id}
                                to={item.path}
                                className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${isActive(item.path)
                                        ? 'text-primary-600 bg-primary-50'
                                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                                    }`}
                            >
                                {item.label}
                            </Link>
                        ))}
                    </nav>
                </div>
            </header>

            <main
                className={`flex-grow w-full mx-auto px-6 py-8 ${wide ? 'max-w-[90vw]' : 'max-w-7xl'
                    }`}
            >
                {children}
            </main>
        </div>
    );
}
