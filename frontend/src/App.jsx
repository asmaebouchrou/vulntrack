import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const [file, setFile] = useState(null);
  const [backendStatus, setBackendStatus] = useState("Comprobando...");

  useEffect(() => {
    async function checkBackend() {
      try {
        const response = await fetch("/api/v1/health");

        if (!response.ok) {
          throw new Error("El backend no responde correctamente");
        }

        setBackendStatus("Conectado");
      } catch (error) {
        setBackendStatus("Desconectado");
      }
    }

    checkBackend();
  }, []);
  function handleFileChange(event) {
    const selectedFile = event.target.files[0];
    setFile(selectedFile);
  }
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [scanResult, setScanResult] = useState(null);

  async function handleAnalyze() {
    if (!file) return;

    if (file.name !== "pom.xml") {
      setError("Selecciona un archivo llamado pom.xml");
      return;
    }

    setLoading(true);
    setError("");
    setScanResult(null);

    try {
      // 1. Obtener el token CSRF de la sesión actual
      const csrfResponse = await fetch("/api/v1/auth/csrf");

      if (!csrfResponse.ok) {
        throw new Error("No se pudo obtener el token CSRF. Inicia sesión.");
      }

      const csrf = await csrfResponse.json();

      // 2. Preparar el archivo para enviarlo
      const formData = new FormData();
      formData.append("file", file);

      // 3. Enviar el archivo al backend
      const response = await fetch("/api/v1/dependencies/upload", {
        method: "POST",
        headers: {
          [csrf.headerName]: csrf.token,
        },
        body: formData,
      });

      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          throw new Error("La sesión no es válida o la petición fue rechazada.");
        }

        throw new Error(`Error al analizar el archivo (HTTP ${response.status}).`);
      }

      const data = await response.json();
      setScanResult(data);
      await loadHistory();
    } catch (err) {
      setError(err.message || "Ha ocurrido un error inesperado.");
    } finally {
      setLoading(false);
    }

  }
  const [history, setHistory] = useState([]);
  const [historyError, setHistoryError] = useState("");
  async function loadHistory() {
    try {
      const response = await fetch("/api/v1/scans");

      if (!response.ok) {
        throw new Error(`Error HTTP ${response.status}`);
      }

      const data = await response.json();

      setHistory(data);
      setHistoryError("");
    } catch (error) {
      setHistoryError("No se pudo cargar el historial");
    }
  }
  useEffect(() => {
    loadHistory();
  }, []);
  async function viewScan(scanId) {
    setError("");
    setScanResult(null);

    try {
      const response = await fetch(`/api/v1/scans/${scanId}`);

      if (!response.ok) {
        throw new Error("No se pudo recuperar el análisis");
      }

      const data = await response.json();

      setScanResult({
        scanId: data.id,
        results: data.dependencies,
      });
    } catch (err) {
      setError(err.message);
    }
  }
  function getSeverity(vulnerability) {
    const severity = vulnerability.database_specific?.severity;

    const translations = {
      CRITICAL: "Crítica",
      HIGH: "Alta",
      MODERATE: "Moderada",
      MEDIUM: "Media",
      LOW: "Baja",
    };

    return translations[severity] ?? "No disponible";
  }
  function getFixedVersions(vulnerability, dependency) {
    const packageName = `${dependency.groupId}:${dependency.artifactId}`;

    const versions = (vulnerability.affected ?? [])
      .filter((affected) => affected.package?.name === packageName)
      .flatMap((affected) => affected.ranges ?? [])
      .filter((range) => range.type === "ECOSYSTEM")
      .flatMap((range) => range.events ?? [])
      .map((event) => event.fixed)
      .filter(Boolean);

    return [...new Set(versions)];
  }
  return (
    <div className="app">
      <header className="header">
        <h1>VulnTrack</h1>
        <p>Analizador de vulnerabilidades en dependencias Maven</p>
        <p>Estado del backend: {backendStatus}</p>
        <a href="/login" className="login-button">
          Iniciar sesión
        </a>
      </header>

      <main>
        <section className="card">
          <h2>Nuevo análisis</h2>
          <p>Selecciona un archivo pom.xml para analizar sus dependencias.</p>

          <input
            type="file"
            accept=".xml"
            onChange={handleFileChange}
          />

          <p>
            Archivo seleccionado: {file ? file.name : "Ninguno"}
          </p>

          <button
            onClick={handleAnalyze}
            disabled={!file || loading}
          >
            {loading ? "Analizando..." : "Analizar dependencias"}
          </button>
        </section>

        <section className="card">
          <h2>Resultados</h2>

          {error && <p role="alert" style={{ color: "crimson" }}>{error}</p>}

          {!scanResult && !error && (
            <p>Aquí aparecerán las vulnerabilidades encontradas.</p>
          )}

          {scanResult && (
            <>
              <p>Análisis guardado con ID: {scanResult.scanId}</p>

              {(scanResult.results ?? []).length === 0 ? (
                <p>No hay resultados de vulnerabilidades para mostrar.</p>
              ) : (
                (scanResult.results ?? []).map((dependency, index) => (
                  <div className="dependency-result" key={index}>
                    <h3>
                      {dependency.groupId}:{dependency.artifactId}
                    </h3>

                    <p>Versión: {dependency.version ?? "No especificada"}</p>

                    <p>
                      Vulnerabilidades conocidas:{" "}
                      {dependency.vulnerabilities?.length ?? 0}
                    </p>

                    {(dependency.vulnerabilities ?? []).map((vulnerability, i) => (
                      <div className="vulnerability-card" key={vulnerability.id ?? i}>

                        <h4>{vulnerability.id}</h4>
                        <p>
                          <strong>CVE: </strong>
                          {vulnerability.aliases?.find((alias) =>
                            alias.startsWith("CVE-")
                          ) ?? "No disponible"}
                        </p>
                        <p>
                          <strong>Descripción: </strong>
                          {vulnerability.summary ||
                            vulnerability.details ||
                            "Descripción no disponible"}
                        </p>

                        <p>
                          <strong>Gravedad: </strong>
                          {getSeverity(vulnerability)}
                        </p>
                        <p>
                          <strong>Versiones de corrección indicadas por OSV: </strong>
                          {getFixedVersions(vulnerability, dependency).join(", ") ||
                            "No disponibles"}
                        </p>
                        {vulnerability.references?.length > 0 && (
                          <a
                            href={vulnerability.references[0].url}
                            target="_blank"
                            rel="noopener noreferrer"
                          >
                            Consultar aviso de seguridad
                          </a>
                        )}

                      </div>
                    ))}
                  </div>
                ))
              )}
            </>
          )}
        </section>

        <section className="card">
          <h2>Historial de análisis</h2>

          {historyError && <p role="alert">{historyError}</p>}

          {history.length === 0 && !historyError ? (
            <p>Todavía no hay análisis guardados.</p>
          ) : (
            history.map((scan) => (
              <div className="dependency-result" key={scan.id}>
                <h3>{scan.filename}</h3>

                <p>ID: {scan.id}</p>

                <p>
                  Fecha: {new Date(scan.createdAt).toLocaleString("es-ES")}
                </p>

                <p>Dependencias: {scan.dependencyCount}</p>

                <p>
                  Dependencias vulnerables: {scan.vulnerableDependencyCount}
                </p>
                <button onClick={() => viewScan(scan.id)}>
                  Ver análisis
                </button>
              </div>
            ))
          )}
        </section>
      </main>
    </div>
  );


}

export default App;