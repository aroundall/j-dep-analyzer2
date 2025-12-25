import { useState, useRef } from 'react';
import type { UploadResult } from '../types/api';
import { uploadPoms } from '../api/client';

interface FileUploadProps {
    onUploadComplete?: (result: UploadResult) => void;
}

export function FileUpload({ onUploadComplete }: FileUploadProps) {
    const [fileCount, setFileCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<UploadResult | null>(null);
    const [error, setError] = useState<string | null>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFileCount(e.target.files?.length ?? 0);
        setResult(null);
        setError(null);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        const files = inputRef.current?.files;
        if (!files || files.length === 0) return;

        setLoading(true);
        setError(null);

        try {
            const uploadResult = await uploadPoms(files);
            setResult(uploadResult);
            onUploadComplete?.(uploadResult);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Upload failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="md:col-span-2 bg-surface rounded-xl shadow-mate-1 border border-gray-100 p-8 flex flex-col justify-center">
            <form onSubmit={handleSubmit} className="flex flex-col items-center gap-4">
                <div className="w-full text-center border-2 border-dashed border-gray-300 rounded-xl p-8 hover:bg-gray-50 transition-colors cursor-pointer relative group">
                    <input
                        ref={inputRef}
                        type="file"
                        name="files"
                        multiple
                        accept=".xml,.pom"
                        className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
                        onChange={handleFileChange}
                    />
                    <div className="flex flex-col items-center gap-2 text-gray-500 group-hover:text-primary-600 transition-colors">
                        <span className="material-symbols-outlined text-4xl">cloud_upload</span>
                        <p className="text-lg font-medium">Drag & drop POMs here</p>
                        <p className="text-sm">or click to browse</p>
                    </div>
                </div>

                <div className="flex items-center justify-between w-full">
                    <span className="text-sm text-gray-500 font-medium">
                        {fileCount > 0 ? `${fileCount} files selected` : 'No files selected'}
                    </span>

                    <button
                        type="submit"
                        disabled={fileCount === 0 || loading}
                        className="ripple bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-6 rounded-lg shadow-sm shadow-primary-500/30 flex items-center gap-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <span className="material-symbols-outlined text-sm">upload</span>
                        Upload & Analyze
                        {loading && (
                            <span className="material-symbols-outlined animate-spin text-sm">
                                progress_activity
                            </span>
                        )}
                    </button>
                </div>
            </form>

            {/* Status Display */}
            {result && (
                <div className="mt-4">
                    {result.success ? (
                        <div className="p-4 rounded-lg bg-green-50 border border-green-200">
                            <p className="text-green-700 font-medium">Upload Complete</p>
                            <ul className="text-sm text-green-600 mt-2">
                                <li>Parsed: {result.parsed} files</li>
                                <li>New artifacts: {result.newArtifacts}</li>
                                <li>New edges: {result.newEdges}</li>
                                {result.skipped > 0 && (
                                    <li className="text-yellow-600">Skipped: {result.skipped}</li>
                                )}
                            </ul>
                            {result.errors && result.errors.length > 0 && (
                                <div className="mt-2 text-red-500 text-sm">
                                    <p className="font-medium">Errors:</p>
                                    {result.errors.map((err, i) => (
                                        <p key={i}>{err}</p>
                                    ))}
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="p-4 rounded-lg bg-red-50 border border-red-200">
                            <p className="text-red-700 font-medium">Upload Failed</p>
                            <p className="text-sm text-red-600">{result.errors?.[0] || 'Unknown error'}</p>
                        </div>
                    )}
                </div>
            )}

            {error && (
                <div className="mt-4 p-4 rounded-lg bg-red-50 border border-red-200">
                    <p className="text-red-700 font-medium">Upload Failed</p>
                    <p className="text-sm text-red-600">{error}</p>
                </div>
            )}
        </div>
    );
}
