import { useEffect, useRef, useCallback } from 'react';
import cytoscape from 'cytoscape';
import type { Core } from 'cytoscape';
import dagre from 'cytoscape-dagre';
import type { CytoscapeElement } from '../types/api';

// Register dagre layout
cytoscape.use(dagre);

export type LayoutName = 'dagre' | 'cose' | 'grid' | 'circle' | 'concentric' | 'breadthfirst';

interface CytoscapeGraphProps {
    elements: CytoscapeElement[];
    layout?: LayoutName;
    onNodeClick?: (nodeId: string) => void;
    rootId?: string;
    className?: string;
}

const baseNodeStyle: cytoscape.Css.Node = {
    'background-color': '#6366f1',
    'label': 'data(label)',
    'color': '#1e293b',
    'font-size': '12px',
    'font-family': 'Inter, sans-serif',
    'text-valign': 'center',
    'text-halign': 'center',
    'width': 'label',
    'height': 'label',
    'padding': '12px',
    'shape': 'round-rectangle',
    'text-wrap': 'wrap',
    'text-max-width': '120px',
    'border-width': 1,
    'border-color': '#e0e7ff',
    'background-opacity': 0.1,
};

const edgeStyle: cytoscape.Css.Edge = {
    'width': 1,
    'line-color': '#cbd5e1',
    'target-arrow-color': '#cbd5e1',
    'target-arrow-shape': 'triangle',
    'curve-style': 'bezier',
};

export function CytoscapeGraph({
    elements,
    layout = 'concentric',
    onNodeClick,
    rootId,
    className = '',
}: CytoscapeGraphProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const cyRef = useRef<Core | null>(null);

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const getLayoutConfig = useCallback((name: LayoutName): any => {
        const base = {
            name,
            animate: true,
            animationDuration: 500,
            padding: 50,
            fit: true,
        };

        if (name === 'dagre') {
            return { ...base, rankDir: 'LR' };
        }
        return base;
    }, []);

    // Initialize Cytoscape
    useEffect(() => {
        if (!containerRef.current || elements.length === 0) return;

        const cy = cytoscape({
            container: containerRef.current,
            elements: elements,
            minZoom: 0.1,
            maxZoom: 3,
            wheelSensitivity: 0.3,
            style: [
                { selector: 'node', style: baseNodeStyle },
                {
                    selector: 'node.root',
                    style: {
                        'background-color': '#dc2626',
                        'background-opacity': 0.2,
                        'border-color': '#dc2626',
                        'border-width': 2,
                        'font-weight': 'bold',
                    },
                },
                {
                    selector: 'node.highlight',
                    style: {
                        'background-color': '#f59e0b',
                        'background-opacity': 0.2,
                        'border-color': '#f59e0b',
                    },
                },
                { selector: 'edge', style: edgeStyle },
                {
                    selector: ':selected',
                    style: {
                        'background-color': '#4f46e5',
                        'line-color': '#4f46e5',
                        'target-arrow-color': '#4f46e5',
                        'source-arrow-color': '#4f46e5',
                        'color': '#4f46e5',
                        'font-weight': 'bold',
                        'border-color': '#4f46e5',
                        'border-width': 2,
                    },
                },
            ],
            layout: getLayoutConfig(layout),
        });

        cyRef.current = cy;

        // Add root class if specified
        if (rootId) {
            cy.nodes(`[id = "${rootId}"]`).addClass('root');
        }

        // Handle node click
        if (onNodeClick) {
            cy.on('tap', 'node', (evt) => {
                const nodeId = evt.target.id();
                if (nodeId && nodeId !== rootId) {
                    onNodeClick(nodeId);
                }
            });
        }

        // Fit after layout completes
        cy.on('layoutstop', () => {
            cy.fit(undefined, 50);
        });

        return () => {
            cy.destroy();
            cyRef.current = null;
        };
    }, [elements, layout, onNodeClick, rootId, getLayoutConfig]);

    // Handle layout changes
    const updateLayout = useCallback((name: LayoutName) => {
        if (cyRef.current) {
            cyRef.current.layout(getLayoutConfig(name)).run();
        }
    }, [getLayoutConfig]);

    // Expose fit function
    const fit = useCallback(() => {
        if (cyRef.current) {
            cyRef.current.fit(undefined, 50);
        }
    }, []);

    // Expose methods via ref-like pattern (using data attribute)
    useEffect(() => {
        if (containerRef.current) {
            (containerRef.current as unknown as { updateLayout: typeof updateLayout; fit: typeof fit }).updateLayout = updateLayout;
            (containerRef.current as unknown as { updateLayout: typeof updateLayout; fit: typeof fit }).fit = fit;
        }
    }, [updateLayout, fit]);

    if (elements.length === 0) {
        return (
            <div className={`flex h-full items-center justify-center text-gray-400 ${className}`}>
                No dependency data found. Upload a POM file to start.
            </div>
        );
    }

    return <div ref={containerRef} className={`w-full h-full ${className}`} />;
}

// Helper to get graph container methods
export function getGraphMethods(container: HTMLDivElement | null) {
    if (!container) return null;
    const c = container as unknown as { updateLayout?: (name: LayoutName) => void; fit?: () => void };
    return {
        updateLayout: c.updateLayout,
        fit: c.fit,
    };
}
