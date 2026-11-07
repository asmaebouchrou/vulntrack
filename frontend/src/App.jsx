import { useCallback, useEffect, useRef, useState } from "react";
import {
  Header,
  UploadSection,
  ScanHistory,
  Icon,
} from "./components/Workspace";
import ScanResults from "./components/ScanResults";
import "./App.css";

class SessionError extends Error {}

async function readJson(response, fallback) {
  const contentType = response.headers.get("content-type") ?? "";
  if (
    response.status === 401 ||
    (response.redirected && new URL(response.url).pathname === "/login")
  ) {
    throw new SessionError(
      "Inicia sesión para continuar. Si ya habías iniciado sesión, puede haber caducado.",
    );
  }
  if (response.status === 403)
    throw new Error(
      "La petición no se ha autorizado. Vuelve a iniciar sesión e inténtalo de nuevo.",
    );
  if (!response.ok) {
    const data = contentType.includes("json")
      ? await response.json().catch(() => null)
      : null;
    throw new Error(data?.error || `${fallback} (HTTP ${response.status}).`);
  }
  if (!contentType.includes("json"))
    throw new Error(
      "El servidor no ha devuelto una respuesta válida. Vuelve a intentarlo.",
    );
  return response.json();
}

export default function App() {
  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState("");
  const [backendStatus, setBackendStatus] = useState("checking");
  const [authStatus, setAuthStatus] = useState("checking");
  const [loggingOut, setLoggingOut] = useState(false);
  const [authError, setAuthError] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [scanResult, setScanResult] = useState(null);
  const [metadata, setMetadata] = useState(null);
  const [history, setHistory] = useState([]);
  const [historyError, setHistoryError] = useState("");
  const [historyLoading, setHistoryLoading] = useState(true);
  const [openingId, setOpeningId] = useState(null);
  const resultsHeading = useRef(null);
  const operationInProgress = useRef(false);
  const busy = loading || openingId !== null || loggingOut;

  useEffect(() => {
    const controller = new AbortController();
    async function checkBackend() {
      try {
        const response = await fetch("/api/v1/health", {
          signal: controller.signal,
        });
        setBackendStatus(response.ok ? "connected" : "disconnected");
      } catch (exception) {
        if (exception.name !== "AbortError") setBackendStatus("disconnected");
      }
    }
    checkBackend();
    return () => controller.abort();
  }, []);

  const loadHistory = useCallback((signal) => {
    return fetch("/api/v1/scans", { credentials: "same-origin", signal })
      .then((response) => readJson(response, "No se pudo cargar el historial"))
      .then((data) => {
        if (!Array.isArray(data))
          throw new Error("No se pudo interpretar el historial del servidor.");
        setHistory(data);
        setHistoryError("");
        setAuthStatus("authenticated");
      })
      .catch((exception) => {
        if (exception.name === "AbortError") return;
        setHistoryError(
          exception instanceof SessionError
            ? ""
            : "No se pudo cargar el historial. Comprueba la conexión.",
        );
        if (exception instanceof SessionError) {
          setAuthStatus("anonymous");
          setHistory([]);
        } else
          setAuthStatus((current) =>
            current === "checking" ? "unknown" : current,
          );
      })
      .finally(() => {
        if (!signal?.aborted) setHistoryLoading(false);
      });
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    loadHistory(controller.signal);
    return () => controller.abort();
  }, [loadHistory]);

  function handleFile(files) {
    if (!files.length) return;
    if (files.length !== 1) {
      setFile(null);
      setFileError("Selecciona un único archivo pom.xml.");
      return;
    }
    const selected = files[0];
    setFile(selected);
    setFileError(
      selected.name !== "pom.xml"
        ? "El archivo debe llamarse pom.xml."
        : selected.size === 0
          ? "El archivo está vacío. Selecciona un pom.xml con contenido."
          : "",
    );
    setError("");
  }

  function focusResults() {
    requestAnimationFrame(() => {
      resultsHeading.current?.focus({ preventScroll: true });
      resultsHeading.current?.scrollIntoView({
        block: "start",
        behavior: "instant",
      });
    });
  }

  async function handleAnalyze() {
    if (
      !file ||
      fileError ||
      operationInProgress.current ||
      authStatus !== "authenticated"
    )
      return;
    operationInProgress.current = true;
    setLoading(true);
    setError("");
    setScanResult(null);
    try {
      const csrf = await readJson(
        await fetch("/api/v1/auth/csrf", { credentials: "same-origin" }),
        "No se pudo obtener el token de seguridad",
      );
      const formData = new FormData();
      formData.append("file", file);
      const response = await fetch("/api/v1/dependencies/upload", {
        method: "POST",
        credentials: "same-origin",
        headers: { [csrf.headerName]: csrf.token },
        body: formData,
      });
      const data = await readJson(response, "No se pudo analizar el archivo");
      setScanResult(data);
      setMetadata({ filename: file.name });
      setAuthStatus("authenticated");
      setHistoryLoading(true);
      await loadHistory();
    } catch (exception) {
      setError(
        exception.message ||
          "No se pudo completar el análisis. Inténtalo de nuevo.",
      );
      if (exception instanceof SessionError) {
        setAuthStatus("anonymous");
        setHistory([]);
      }
    } finally {
      setLoading(false);
      operationInProgress.current = false;
      focusResults();
    }
  }

  async function viewScan(scanId) {
    if (operationInProgress.current) return;
    operationInProgress.current = true;
    setOpeningId(scanId);
    setError("");
    setScanResult(null);
    try {
      const data = await readJson(
        await fetch(`/api/v1/scans/${scanId}`, { credentials: "same-origin" }),
        "No se pudo recuperar el análisis",
      );
      setScanResult({ scanId: data.id, results: data.dependencies });
      setMetadata({ filename: data.filename, createdAt: data.createdAt });
      setAuthStatus("authenticated");
    } catch (exception) {
      setError(exception.message || "No se pudo recuperar el análisis.");
      if (exception instanceof SessionError) {
        setAuthStatus("anonymous");
        setHistory([]);
      }
    } finally {
      setOpeningId(null);
      operationInProgress.current = false;
      focusResults();
    }
  }

  async function handleLogout() {
    if (operationInProgress.current) return;
    operationInProgress.current = true;
    setLoggingOut(true);
    setAuthError("");
    try {
      const csrf = await readJson(
        await fetch("/api/v1/auth/csrf", { credentials: "same-origin" }),
        "No se pudo preparar el cierre de sesión",
      );
      const response = await fetch("/logout", {
        method: "POST",
        credentials: "same-origin",
        headers: { [csrf.headerName]: csrf.token },
      });
      if (!response.ok)
        throw new Error("No se pudo cerrar la sesión. Inténtalo de nuevo.");
      setAuthStatus("anonymous");
      setHistory([]);
      setHistoryError("");
      setScanResult(null);
      setError("");
    } catch (exception) {
      if (exception instanceof SessionError) {
        setAuthStatus("anonymous");
        setHistory([]);
        setScanResult(null);
      } else setAuthError(exception.message || "No se pudo cerrar la sesión.");
    } finally {
      setLoggingOut(false);
      operationInProgress.current = false;
    }
  }

  function refreshHistory() {
    if (busy || historyLoading) return;
    setHistoryLoading(true);
    loadHistory();
  }

  return (
    <>
      <a className="skip-link" href="#contenido">
        Saltar al contenido
      </a>
      <Header
        backendStatus={backendStatus}
        authStatus={authStatus}
        loggingOut={loggingOut}
        busy={busy}
        onLogout={handleLogout}
        authError={authError}
      />
      <main className="app" id="contenido">
        <div className="page-heading">
          <div>
            <p className="eyebrow">REVISIÓN DE SEGURIDAD</p>
            <h1>Análisis de dependencias</h1>
            <p>
              Identifica vulnerabilidades conocidas y consulta el historial de
              tus proyectos.
            </p>
          </div>
        </div>
        <nav className="section-nav" aria-label="Secciones del análisis">
          <a href="#nuevo-analisis">
            <Icon name="upload" />
            Nuevo análisis
          </a>
          <a href="#resultados">
            <Icon name="layers" />
            Resultados
          </a>
          <a href="#historial">
            <Icon name="clock" />
            Historial
          </a>
        </nav>
        <UploadSection
          file={file}
          fileError={fileError}
          onFile={handleFile}
          loading={loading}
          busy={busy}
          authStatus={authStatus}
          onAnalyze={handleAnalyze}
        />
        <ScanResults
          result={scanResult}
          metadata={metadata}
          loading={loading}
          openingId={openingId}
          error={error}
          headingRef={resultsHeading}
        />
        <ScanHistory
          history={history}
          loading={historyLoading}
          error={historyError}
          authStatus={authStatus}
          onReload={refreshHistory}
          onView={viewScan}
          openingId={openingId}
          busy={busy}
          selectedId={scanResult?.scanId}
        />
        <footer className="site-footer">
          <span>
            <Icon name="shield" />
            VulnTrack
          </span>
          <p>
            Análisis de dependencias Maven · Información de vulnerabilidades de
            OSV
          </p>
        </footer>
      </main>
    </>
  );
}
