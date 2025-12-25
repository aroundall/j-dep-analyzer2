import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { getDependencies, getScopes, getDependenciesExportUrl } from '../api/client';
import type { DependencyRow, DependencyParams } from '../types/api';

export function DependenciesList() {
    const navigate = useNavigate();
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const [dependencies, setDependencies] = useState<DependencyRow[]>([]);
    const [scopes, setScopes] = useState<string[]>([]);
    const [loading, setLoading] = useState(false);

    // Filters
    const [query, setQuery] = useState('');
    const [groupQuery, setGroupQuery] = useState('');
    const [selectedScopes, setSelectedScopes] = useState<string[]>([]);
    const [ignoreVersion, setIgnoreVersion] = useState(false);
    const [ignoreGroup, setIgnoreGroup] = useState(false);
    const [showScopeDropdown, setShowScopeDropdown] = useState(false);

    const fetchDependencies = useCallback(async (params: DependencyParams) => {
        setLoading(true);
        try {
            const data = await getDependencies(params);
            setDependencies(data);
        } catch (err) {
            console.error('Failed to load dependencies:', err);
        } finally {
            setLoading(false);
        }
    }, []);

    // Initial load
    useEffect(() => {
        getScopes().then(setScopes).catch(console.error);
        fetchDependencies({});
    }, [fetchDependencies]);

    // Debounced filter update
    const updateFilters = useCallback(() => {
        fetchDependencies({
            q: query || undefined,
            groupQ: groupQuery || undefined,
            scope: selectedScopes.length > 0 ? selectedScopes : undefined,
            ignoreVersion,
            ignoreGroup,
        });
    }, [fetchDependencies, query, groupQuery, selectedScopes, ignoreVersion, ignoreGroup]);

    const handleInputChange = (setter: (v: string) => void) => (e: React.ChangeEvent<HTMLInputElement>) => {
        setter(e.target.value);
        if (debounceRef.current !== null) {
            clearTimeout(debounceRef.current);
        }
        debounceRef.current = setTimeout(updateFilters, 300);
    };

    const handleScopeToggle = (scope: string) => {
        setSelectedScopes((prev) =>
            prev.includes(scope) ? prev.filter((s) => s !== scope) : [...prev, scope]
        );
    };

    useEffect(() => {
        updateFilters();
    }, [selectedScopes, ignoreVersion, ignoreGroup, updateFilters]);

    const handleExport = () => {
        const url = getDependenciesExportUrl({
            q: query || undefined,
            groupQ: groupQuery || undefined,
            scope: selectedScopes.length > 0 ? selectedScopes : undefined,
            ignoreVersion,
            ignoreGroup,
        });
        window.location.href = url;
    };

    const handleRowDoubleClick = (fromGav: string) => {
        navigate(`/visualize/${encodeURIComponent(fromGav)}`);
    };

    return (
        <Layout>
            <div className="space-y-6">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <div>
                        <h2 className="text-2xl font-bold text-gray-900">Dependencies List</h2>
                        <p className="text-gray-500 text-sm mt-1">Browse and filter all dependency pairs</p>
                    </div>
                </div>

                {/* Filters Card */}
                <div className="bg-surface rounded-xl shadow-mate-1 border border-gray-100 p-6">
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Filter by Artifact ID
                            </label>
                            <input
                                type="text"
                                value={query}
                                onChange={handleInputChange(setQuery)}
                                placeholder="e.g. spring-core"
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-primary-500 focus:border-primary-500 text-sm"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Filter by Group ID
                            </label>
                            <input
                                type="text"
                                value={groupQuery}
                                onChange={handleInputChange(setGroupQuery)}
                                placeholder="e.g. org.springframework"
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-primary-500 focus:border-primary-500 text-sm"
                            />
                        </div>

                        <div className="relative">
                            <label className="block text-sm font-medium text-gray-700 mb-1">Scope</label>
                            <button
                                type="button"
                                onClick={() => setShowScopeDropdown(!showScopeDropdown)}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-primary-500 focus:border-primary-500 text-sm bg-white text-left flex items-center justify-between"
                            >
                                <span className={selectedScopes.length === 0 ? 'text-gray-400' : 'text-gray-700'}>
                                    {selectedScopes.length === 0 ? 'Select scopes...' : selectedScopes.join(', ')}
                                </span>
                                <span className="material-symbols-outlined text-gray-400 text-sm">expand_more</span>
                            </button>
                            {showScopeDropdown && (
                                <div className="absolute z-10 mt-1 w-full bg-white border border-gray-300 rounded-lg shadow-lg py-1 max-h-48 overflow-auto">
                                    {scopes.map((scope) => (
                                        <div key={scope} className="px-3 py-2 hover:bg-gray-50">
                                            <label className="flex items-center gap-2 cursor-pointer">
                                                <input
                                                    type="checkbox"
                                                    checked={selectedScopes.includes(scope)}
                                                    onChange={() => handleScopeToggle(scope)}
                                                    className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                                                />
                                                <span className="text-sm text-gray-700">{scope}</span>
                                            </label>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        <div className="flex items-end gap-4">
                            <label className="flex items-center gap-2 cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={ignoreVersion}
                                    onChange={(e) => setIgnoreVersion(e.target.checked)}
                                    className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                                />
                                <span className="text-sm text-gray-600">Combine Versions</span>
                            </label>
                            <label className="flex items-center gap-2 cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={ignoreGroup}
                                    onChange={(e) => setIgnoreGroup(e.target.checked)}
                                    className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                                />
                                <span className="text-sm text-gray-600">Combine Groups</span>
                            </label>
                        </div>
                    </div>
                </div>

                {/* Table Card */}
                <div className="bg-surface rounded-xl shadow-mate-1 border border-gray-100 overflow-hidden">
                    <div className="px-6 py-4 border-b border-gray-100 bg-gray-50 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <span className="material-symbols-outlined text-primary-600">list</span>
                            <h3 className="font-semibold text-gray-900">Dependency Pairs</h3>
                            {loading && (
                                <span className="material-symbols-outlined animate-spin text-gray-400">
                                    progress_activity
                                </span>
                            )}
                        </div>
                        <button
                            onClick={handleExport}
                            className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
                        >
                            <span className="material-symbols-outlined text-sm">download</span>
                            Export CSV
                        </button>
                    </div>

                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th colSpan={3} className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider border-r border-gray-200">
                                        Source (depends on)
                                    </th>
                                    <th colSpan={4} className="px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                                        Target (dependency)
                                    </th>
                                </tr>
                                <tr>
                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Group</th>
                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Artifact</th>
                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 border-r border-gray-200">Version</th>
                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Group</th>
                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Artifact</th>
                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Version</th>
                                    <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Scope</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {dependencies.length === 0 ? (
                                    <tr>
                                        <td colSpan={7} className="px-4 py-8 text-center text-gray-400">
                                            No dependencies found.
                                        </td>
                                    </tr>
                                ) : (
                                    dependencies.map((row, i) => (
                                        <tr
                                            key={i}
                                            className="hover:bg-gray-50 cursor-pointer"
                                            onDoubleClick={() => handleRowDoubleClick(row.fromGav)}
                                        >
                                            <td className="px-4 py-2 text-sm text-gray-500 font-mono">{row.fromGroup}</td>
                                            <td className="px-4 py-2 text-sm text-gray-900">{row.fromArtifact}</td>
                                            <td className="px-4 py-2 text-sm text-gray-500 border-r border-gray-200">{row.fromVersion}</td>
                                            <td className="px-4 py-2 text-sm text-gray-500 font-mono">{row.toGroup}</td>
                                            <td className="px-4 py-2 text-sm text-gray-900">{row.toArtifact}</td>
                                            <td className="px-4 py-2 text-sm text-gray-500">{row.toVersion}</td>
                                            <td className="px-4 py-2 text-sm text-gray-500">{row.scope}</td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </Layout>
    );
}
