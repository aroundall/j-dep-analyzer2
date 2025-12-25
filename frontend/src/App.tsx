import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Dashboard } from './pages/Dashboard';
import { DependenciesList } from './pages/DependenciesList';
import { Visualize } from './pages/Visualize';
import { Export } from './pages/Export';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/dependencies" element={<DependenciesList />} />
        <Route path="/visualize/:gav" element={<Visualize />} />
        <Route path="/export" element={<Export />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
