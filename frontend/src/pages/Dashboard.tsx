import { useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { FileUpload } from '../components/FileUpload';
import { CytoscapeGraph, getGraphMethods, type LayoutName } from '../components/CytoscapeGraph';
import { getGraphData } from '../api/client';
import type { CytoscapeElement, UploadResult } from '../types/api';

export function Dashboard() {
    const navigate = useNavigate();
    const graphContainerRef = useRef<HTMLDivElement>(null);

    const [elements, setElements] = useState<CytoscapeElement[]>([]);
    const [showGroup, setShowGroup] = useState(false);
    const [showVersion, setShowVersion] = useState(false);
    const [layout, setLayout] = useState<LayoutName>('concentric');
    const [loading, setLoading] = useState(false);

    const refreshGraph = useCallback(async (ignoreGroup: boolean, ignoreVersion: boolean) => {
        setLoading(true);
        try {
            const data = await getGraphData({
                showGroup: !ignoreGroup,
                showVersion: !ignoreVersion,
                direction: 'forward',
            });
            setElements(data.elements || []);
        } catch (err) {
            console.error('Failed to load graph:', err);
        } finally {
            setLoading(false);
        }
    }, []);

    // Initial load
    useState(() => {
        refreshGraph(showGroup, showVersion);
    });

    const handleToggleGroup = () => {
        const newValue = !showGroup;
        setShowGroup(newValue);
        refreshGraph(newValue, showVersion);
    };

    const handleToggleVersion = () => {
        const newValue = !showVersion;
        setShowVersion(newValue);
        refreshGraph(showGroup, newValue);
    };

    const handleLayoutChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const newLayout = e.target.value as LayoutName;
        setLayout(newLayout);
        getGraphMethods(graphContainerRef.current)?.updateLayout?.(newLayout);
    };

    const handleFit = () => {
        getGraphMethods(graphContainerRef.current)?.fit?.();
    };

    const handleNodeClick = (nodeId: string) => {
        navigate(`/visualize/${encodeURIComponent(nodeId)}`);
    };

    const handleUploadComplete = (result: UploadResult) => {
        if (result.success) {
            setTimeout(() => refreshGraph(showGroup, showVersion), 500);
        }
    };

    return (
        <Layout>
            <div className="space-y-8">
                {/* Welcome / Upload Section */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    {/* Intro Card */}
                    <div className="md:col-span-1 bg-indigo-600 rounded-2xl shadow-mate-3 p-8 text-white relative overflow-hidden flex flex-col justify-center">
                        <div className="absolute top-0 right-0 -mt-10 -mr-10 w-64 h-64 bg-white opacity-10 rounded-full blur-3xl" />
                        <div className="absolute bottom-0 left-0 -mb-10 -ml-10 w-40 h-40 bg-indigo-400 opacity-20 rounded-full blur-2xl" />
                        <div className="relative z-10">
                            <h2 className="text-3xl font-bold mb-2">Welcome</h2>
                            <p className="text-indigo-100 mb-6">
                                Upload your <code>pom.xml</code> files to analyze dependency trees and
                                identify conflicts across your microservices.
                            </p>
                        </div>
                    </div>

                    {/* Upload Widget */}
                    <FileUpload onUploadComplete={handleUploadComplete} />
                </div>

                {/* Global Graph Card */}
                <div className="bg-surface rounded-xl shadow-mate-1 border border-gray-100 flex flex-col overflow-hidden h-[800px]">
                    <div className="px-6 py-4 border-b border-gray-100 bg-gray-50 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <span className="material-symbols-outlined text-primary-600">hub</span>
                            <h3 className="font-semibold text-gray-900">Global Dependency Graph</h3>
                            {loading && (
                                <span className="material-symbols-outlined animate-spin text-gray-400">
                                    progress_activity
                                </span>
                            )}
                        </div>

                        {/* Graph Controls */}
                        <div className="flex items-center gap-4 text-sm">
                            <label className="flex items-center gap-2 cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={showGroup}
                                    onChange={handleToggleGroup}
                                    className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                                />
                                <span className="text-gray-600">Combine Groups</span>
                            </label>
                            <label className="flex items-center gap-2 cursor-pointer">
                                <input
                                    type="checkbox"
                                    checked={showVersion}
                                    onChange={handleToggleVersion}
                                    className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                                />
                                <span className="text-gray-600">Combine Versions</span>
                            </label>

                            <div className="h-4 w-px bg-gray-300 mx-2" />

                            <select
                                value={layout}
                                onChange={handleLayoutChange}
                                className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-primary-500 focus:border-primary-500 block p-1.5"
                            >
                                <option value="dagre">Hierarchy (Dagre)</option>
                                <option value="cose">Force (Cose)</option>
                                <option value="grid">Grid</option>
                                <option value="circle">Circle</option>
                                <option value="concentric">Concentric</option>
                                <option value="breadthfirst">Breadthfirst</option>
                            </select>

                            <button
                                onClick={handleFit}
                                className="text-gray-500 hover:text-primary-600"
                                title="Fit to Screen"
                            >
                                <span className="material-symbols-outlined">fit_screen</span>
                            </button>
                        </div>
                    </div>

                    <div ref={graphContainerRef} className="flex-grow">
                        <CytoscapeGraph
                            elements={elements}
                            layout={layout}
                            onNodeClick={handleNodeClick}
                            className="h-full"
                        />
                    </div>
                </div>
            </div>
        </Layout>
    );
}
