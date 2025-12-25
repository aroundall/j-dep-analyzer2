import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { CytoscapeGraph, getGraphMethods, type LayoutName } from '../components/CytoscapeGraph';
import { getGraphData } from '../api/client';
import type { CytoscapeElement } from '../types/api';

function parseGav(gav: string) {
    const parts = gav.split(':');
    return {
        groupId: parts[0] || '',
        artifactId: parts[1] || '',
        version: parts[2] || '',
    };
}

export function Visualize() {
    const { gav } = useParams<{ gav: string }>();
    const navigate = useNavigate();
    const graphContainerRef = useRef<HTMLDivElement>(null);

    const decodedGav = gav ? decodeURIComponent(gav) : '';
    const { groupId, artifactId, version } = parseGav(decodedGav);

    const [elements, setElements] = useState<CytoscapeElement[]>([]);
    const [loading, setLoading] = useState(false);
    const [showGroup, setShowGroup] = useState(true);
    const [showVersion, setShowVersion] = useState(true);
    const [direction, setDirection] = useState<'forward' | 'reverse'>('forward');
    const [depth, setDepth] = useState<number | undefined>(2);
    const [layout, setLayout] = useState<LayoutName>('dagre');

    const refreshGraph = useCallback(async () => {
        if (!decodedGav) return;

        setLoading(true);
        try {
            const data = await getGraphData({
                rootId: decodedGav,
                showGroup: !showGroup,
                showVersion: !showVersion,
                direction,
                depth,
            });
            setElements(data.elements || []);
        } catch (err) {
            console.error('Failed to load graph:', err);
        } finally {
            setLoading(false);
        }
    }, [decodedGav, showGroup, showVersion, direction, depth]);

    useEffect(() => {
        refreshGraph();
    }, [refreshGraph]);

    const handleLayoutChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const newLayout = e.target.value as LayoutName;
        setLayout(newLayout);
        getGraphMethods(graphContainerRef.current)?.updateLayout?.(newLayout);
    };

    const handleFit = () => {
        getGraphMethods(graphContainerRef.current)?.fit?.();
    };

    const handleNodeClick = (nodeId: string) => {
        if (nodeId !== decodedGav) {
            navigate(`/visualize/${encodeURIComponent(nodeId)}`);
        }
    };

    return (
        <Layout wide>
            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                {/* Left Sidebar: Info Card */}
                <div className="lg:col-span-1">
                    <div className="bg-surface rounded-xl shadow-mate-1 border border-gray-100 p-6 sticky top-24">
                        <div className="flex items-center gap-2 mb-4">
                            <span className="material-symbols-outlined text-primary-600">info</span>
                            <h3 className="font-semibold text-gray-900">Artifact Details</h3>
                        </div>

                        <dl className="space-y-3">
                            <div>
                                <dt className="text-xs font-medium text-gray-500 uppercase">Group ID</dt>
                                <dd className="font-mono text-sm text-gray-900 break-all">{groupId}</dd>
                            </div>
                            <div>
                                <dt className="text-xs font-medium text-gray-500 uppercase">Artifact ID</dt>
                                <dd className="font-mono text-sm text-gray-900 break-all">{artifactId}</dd>
                            </div>
                            <div>
                                <dt className="text-xs font-medium text-gray-500 uppercase">Version</dt>
                                <dd className="font-mono text-sm text-gray-900">{version}</dd>
                            </div>
                        </dl>

                        <hr className="my-4 border-gray-200" />

                        <Link
                            to={`/dependencies?q=${encodeURIComponent(artifactId)}`}
                            className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
                        >
                            <span className="material-symbols-outlined text-sm">arrow_back</span>
                            View in Dependencies List
                        </Link>
                    </div>
                </div>

                {/* Right: Graph Card */}
                <div className="lg:col-span-3">
                    <div className="bg-surface rounded-xl shadow-mate-1 border border-gray-100 flex flex-col overflow-hidden h-[700px]">
                        <div className="px-6 py-4 border-b border-gray-100 bg-gray-50 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <span className="material-symbols-outlined text-primary-600">hub</span>
                                <h3 className="font-semibold text-gray-900">Dependency Graph</h3>
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
                                        onChange={(e) => setShowGroup(e.target.checked)}
                                        className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                                    />
                                    <span className="text-gray-600">Combine Groups</span>
                                </label>
                                <label className="flex items-center gap-2 cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={showVersion}
                                        onChange={(e) => setShowVersion(e.target.checked)}
                                        className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                                    />
                                    <span className="text-gray-600">Combine Versions</span>
                                </label>

                                <div className="h-4 w-px bg-gray-300 mx-2" />

                                <select
                                    value={direction}
                                    onChange={(e) => setDirection(e.target.value as 'forward' | 'reverse')}
                                    className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-primary-500 focus:border-primary-500 p-1.5"
                                >
                                    <option value="forward">Forward</option>
                                    <option value="reverse">Reverse (Who uses this?)</option>
                                </select>

                                <select
                                    value={depth ?? ''}
                                    onChange={(e) => setDepth(e.target.value ? Number(e.target.value) : undefined)}
                                    className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-primary-500 focus:border-primary-500 p-1.5"
                                >
                                    <option value="">All</option>
                                    <option value="1">1 Level</option>
                                    <option value="2">2 Levels</option>
                                    <option value="3">3 Levels</option>
                                </select>

                                <select
                                    value={layout}
                                    onChange={handleLayoutChange}
                                    className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-primary-500 focus:border-primary-500 p-1.5"
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
                                rootId={decodedGav}
                                className="h-full"
                            />
                        </div>
                    </div>
                </div>
            </div>
        </Layout>
    );
}
