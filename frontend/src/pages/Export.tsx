import { useState, useEffect } from 'react';
import { Layout } from '../components/Layout';
import { getExportUrl, getArtifacts, getDependencies } from '../api/client';

interface TableInfo {
    name: string;
    rowCount: number;
}

export function Export() {
    const [tables, setTables] = useState<TableInfo[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function loadCounts() {
            try {
                const [artifacts, deps] = await Promise.all([
                    getArtifacts(10000),
                    getDependencies({ limit: 10000 }),
                ]);
                setTables([
                    { name: 'artifact', rowCount: artifacts.length },
                    { name: 'dependencyedge', rowCount: deps.length },
                ]);
            } catch (err) {
                console.error('Failed to load counts:', err);
                setTables([
                    { name: 'artifact', rowCount: 0 },
                    { name: 'dependencyedge', rowCount: 0 },
                ]);
            } finally {
                setLoading(false);
            }
        }
        loadCounts();
    }, []);

    return (
        <Layout>
            <div className="max-w-2xl mx-auto space-y-6">
                {/* Header */}
                <div>
                    <h2 className="text-2xl font-bold text-gray-900">Export Data</h2>
                    <p className="text-gray-500 text-sm mt-1">Download raw data as CSV files</p>
                </div>

                {/* Table List */}
                <div className="bg-surface rounded-xl shadow-mate-1 border border-gray-100 divide-y divide-gray-100">
                    {loading ? (
                        <div className="p-4 flex items-center justify-center">
                            <span className="material-symbols-outlined animate-spin text-gray-400">
                                progress_activity
                            </span>
                        </div>
                    ) : (
                        tables.map((table) => (
                            <div
                                key={table.name}
                                className="p-4 flex items-center justify-between hover:bg-gray-50 transition-colors"
                            >
                                <div className="flex items-center gap-3">
                                    <span className="material-symbols-outlined text-primary-600">table_chart</span>
                                    <div>
                                        <p className="font-medium text-gray-900 font-mono">{table.name}</p>
                                        <p className="text-sm text-gray-500">{table.rowCount} rows</p>
                                    </div>
                                </div>
                                <a
                                    href={getExportUrl(table.name)}
                                    className="inline-flex items-center gap-1 px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white text-sm font-medium rounded-lg transition-colors"
                                >
                                    <span className="material-symbols-outlined text-sm">download</span>
                                    Download CSV
                                </a>
                            </div>
                        ))
                    )}
                </div>

                {/* Info */}
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-800">
                    <div className="flex items-start gap-2">
                        <span className="material-symbols-outlined text-blue-600 mt-0.5">info</span>
                        <div>
                            <p className="font-medium">Export Format</p>
                            <p className="text-blue-700 mt-1">
                                Files are exported as UTF-8 encoded CSV with headers. Large tables may take a
                                moment to generate.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </Layout>
    );
}
