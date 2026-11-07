import { useState } from "react";

export function Icon({ name, className = "" }) {
  const paths = {
    shield: (
      <>
        <path d="M12 3 4.5 6v5.5c0 4.5 3 7.5 7.5 9.5 4.5-2 7.5-5 7.5-9.5V6L12 3Z" />
        <path d="m8.5 12 2.3 2.3 4.7-4.8" />
      </>
    ),
    upload: (
      <>
        <path d="M12 16V4m-4 4 4-4 4 4M4 16v4h16v-4" />
      </>
    ),
    file: (
      <>
        <path d="M14 3H6v18h12V7l-4-4Z" />
        <path d="M14 3v5h4M9 13h6m-6 4h6" />
      </>
    ),
    arrow: <path d="M5 12h14m-5-5 5 5-5 5" />,
    check: <path d="m5 12 4 4L19 6" />,
    clock: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" />
      </>
    ),
    layers: (
      <>
        <path d="m12 3 10 5-10 5L2 8l10-5Zm-10 9 10 5 10-5M2 16l10 5 10-5" />
      </>
    ),
    link: (
      <>
        <path d="M14 4h6v6m0-6L10 14M10 4H4v16h16v-6" />
      </>
    ),
    refresh: (
      <>
        <path d="M20 5v5h-5M4 19v-5h5" />
        <path d="M5.6 8a7 7 0 0 1 11.5-2L20 10M4 14l2.9 4A7 7 0 0 0 18.4 16" />
      </>
    ),
    logout: (
      <>
        <path d="M10 4H4v16h6m4-4 4-4-4-4m-6 4h10" />
      </>
    ),
    info: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 11v5m0-9v1" />
      </>
    ),
  };
  return (
    <svg
      className={`icon ${className}`}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name] ?? paths.info}
    </svg>
  );
}

export function Header({
  backendStatus,
  authStatus,
  loggingOut,
  busy,
  onLogout,
  authError,
}) {
  const connection = {
    checking: "Comprobando conexión",
    connected: "Backend conectado",
    disconnected: "Backend sin conexión",
  };
  return (
    <header className="site-header">
      <div className="header-inner">
        <a className="brand" href="/" aria-label="VulnTrack, inicio">
          <span className="brand-mark">
            <Icon name="shield" />
          </span>
          <span>
            <span className="brand-name">VulnTrack</span>
            <span className="brand-description">
              Seguridad en tus dependencias
            </span>
          </span>
        </a>
        <div className="header-actions">
          <span
            className={`connection connection--${backendStatus}`}
            role="status"
          >
            <span className="status-dot" />
            {connection[backendStatus]}
          </span>
          <span className="header-divider" aria-hidden="true" />
          {authStatus === "authenticated" ? (
            <>
              <span className="session-label">
                <Icon name="check" />
                Sesión iniciada
              </span>
              <button
                className="button button--quiet"
                onClick={onLogout}
                disabled={busy}
              >
                {loggingOut ? (
                  <span className="spinner" />
                ) : (
                  <Icon name="logout" />
                )}
                {loggingOut ? "Cerrando sesión…" : "Cerrar sesión"}
              </button>
            </>
          ) : authStatus === "checking" ? (
            <span className="session-label" role="status">
              Comprobando sesión…
            </span>
          ) : (
            <>
              <span className="session-label">
                {authStatus === "unknown"
                  ? "Sesión sin verificar"
                  : "Sin sesión iniciada"}
              </span>
              <a className="button button--secondary" href="/login">
                Iniciar sesión
                <Icon name="arrow" />
              </a>
            </>
          )}
        </div>
      </div>
      {authError && (
        <p className="header-error" role="alert">
          {authError}
        </p>
      )}
    </header>
  );
}

export function UploadSection({
  file,
  fileError,
  onFile,
  loading,
  busy,
  authStatus,
  onAnalyze,
}) {
  const [dragging, setDragging] = useState(false);
  const ready = file && !fileError && authStatus === "authenticated" && !busy;
  return (
    <section
      className="panel"
      id="nuevo-analisis"
      aria-labelledby="upload-title"
      aria-busy={loading}
    >
      <div className="section-heading">
        <div className="section-title">
          <span className="step-number" aria-hidden="true">
            01
          </span>
          <div>
            <h2 id="upload-title">Nuevo análisis</h2>
            <p>
              Selecciona el <code>pom.xml</code> de tu proyecto Maven.
            </p>
          </div>
        </div>
        <span className="tag">Maven</span>
      </div>
      <form
        className="upload-layout"
        onSubmit={(event) => {
          event.preventDefault();
          if (ready) onAnalyze();
        }}
      >
        <div>
          <label
            className={`dropzone${dragging ? " is-dragging" : ""}${fileError ? " has-error" : ""}${busy ? " is-disabled" : ""}`}
            htmlFor="pom-file"
            onDragOver={(event) => {
              event.preventDefault();
              if (!busy) setDragging(true);
            }}
            onDragLeave={(event) => {
              if (!event.currentTarget.contains(event.relatedTarget))
                setDragging(false);
            }}
            onDrop={(event) => {
              event.preventDefault();
              setDragging(false);
              if (!busy) onFile(event.dataTransfer.files);
            }}
          >
            <span className="upload-icon">
              <Icon name={file ? "file" : "upload"} />
            </span>
            <span className="dropzone-copy">
              <strong>
                {file ? file.name : "Arrastra aquí tu archivo pom.xml"}
              </strong>
              <span>
                {file
                  ? `${new Intl.NumberFormat("es-ES", { maximumFractionDigits: 1 }).format(file.size / 1024)} KB · Puedes seleccionar otro archivo`
                  : "o selecciónalo desde tu equipo"}
              </span>
            </span>
            <span className="file-picker">
              {file ? "Cambiar archivo" : "Seleccionar archivo"}
            </span>
            <input
              className="visually-hidden"
              id="pom-file"
              name="file"
              type="file"
              accept=".xml"
              disabled={busy}
              onChange={(event) => onFile(event.target.files)}
              aria-describedby="file-hint file-error"
              aria-invalid={Boolean(fileError)}
            />
          </label>
          <p className="field-hint" id="file-hint">
            <Icon name="info" />
            Solo archivos con el nombre <code>pom.xml</code>.
          </p>
          <p
            className="field-error"
            id="file-error"
            role={fileError ? "alert" : undefined}
          >
            {fileError}
          </p>
        </div>
        <div className="upload-action">
          <button
            className="button button--primary"
            type="submit"
            disabled={!ready}
            aria-describedby="analyze-hint"
          >
            {loading ? <span className="spinner" /> : <Icon name="shield" />}
            {loading ? "Analizando dependencias…" : "Analizar dependencias"}
            {!loading && <Icon name="arrow" />}
          </button>
          <p id="analyze-hint" className="action-hint" role="status">
            {loading
              ? "Consultando OSV y guardando los resultados. Puede tardar unos instantes."
              : authStatus === "authenticated"
                ? file && !fileError
                  ? "El análisis se guardará en el historial."
                  : "Selecciona un pom.xml válido para continuar."
                : authStatus === "checking"
                  ? "Comprobando tu sesión antes de continuar."
                  : "Inicia sesión para analizar y consultar el historial."}
          </p>
        </div>
      </form>
    </section>
  );
}

export function ScanHistory({
  history,
  loading,
  error,
  authStatus,
  onReload,
  onView,
  openingId,
  busy,
  selectedId,
}) {
  return (
    <section
      className="panel history-panel"
      id="historial"
      aria-labelledby="history-title"
      aria-busy={loading}
    >
      <div className="section-heading">
        <div className="section-title">
          <span className="step-number" aria-hidden="true">
            03
          </span>
          <div>
            <h2 id="history-title">
              Historial de análisis{" "}
              <span className="count-label">{history.length}</span>
            </h2>
            <p>Recupera los resultados de tus análisis guardados.</p>
          </div>
        </div>
        <button
          className="button button--quiet"
          onClick={onReload}
          disabled={loading || busy}
          aria-label="Actualizar historial"
        >
          <Icon name="refresh" className={loading ? "rotating" : ""} />
          <span>Actualizar</span>
        </button>
      </div>
      {loading ? (
        <div className="empty-state compact" role="status">
          <span className="spinner" />
          <p>Cargando el historial…</p>
        </div>
      ) : authStatus === "anonymous" ? (
        <div className="empty-state compact">
          <span className="empty-icon">
            <Icon name="clock" />
          </span>
          <h3>Tu historial, a un paso</h3>
          <p>Inicia sesión para consultar los análisis guardados.</p>
          <a className="text-link" href="/login">
            Iniciar sesión <Icon name="arrow" />
          </a>
        </div>
      ) : error ? (
        <div className="notice notice--error" role="alert">
          <Icon name="info" />
          <p>{error} Puedes volver a intentarlo con «Actualizar».</p>
        </div>
      ) : history.length === 0 ? (
        <div className="empty-state compact">
          <span className="empty-icon">
            <Icon name="clock" />
          </span>
          <h3>Aún no hay análisis guardados</h3>
          <p>
            Cuando analices tu primer pom.xml, podrás volver a sus resultados
            desde aquí.
          </p>
        </div>
      ) : (
        <>
          <div className="history-columns" aria-hidden="true">
            <span>Archivo</span>
            <span>Fecha y hora</span>
            <span>Dependencias</span>
            <span>Vulnerables</span>
            <span />
          </div>
          <ul className="history-list">
            {history.map((scan) => {
              const date = new Date(scan.createdAt);
              const validDate = !Number.isNaN(date.getTime());
              const selected = selectedId === scan.id;
              return (
                <li
                  className={`history-row${selected ? " is-selected" : ""}`}
                  key={scan.id}
                >
                  <div className="history-file">
                    <span className="document-icon">
                      <Icon name="file" />
                    </span>
                    <div>
                      <strong>{scan.filename}</strong>
                      <span>
                        Análisis #{scan.id}
                        {selected ? " · En pantalla" : ""}
                      </span>
                    </div>
                  </div>
                  <time
                    className="history-date"
                    dateTime={validDate ? scan.createdAt : undefined}
                  >
                    {validDate ? (
                      <>
                        {date.toLocaleDateString("es-ES", {
                          day: "2-digit",
                          month: "short",
                          year: "numeric",
                        })}
                        <span>
                          {date.toLocaleTimeString("es-ES", {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </span>
                      </>
                    ) : (
                      "Fecha no disponible"
                    )}
                  </time>
                  <span className="history-metric">
                    <span className="metric-label">Dependencias</span>
                    <strong>{scan.dependencyCount}</strong>
                  </span>
                  <span className="history-metric">
                    <span className="metric-label">Vulnerables</span>
                    <strong
                      className={
                        scan.vulnerableDependencyCount > 0
                          ? "vulnerable-count"
                          : ""
                      }
                    >
                      {scan.vulnerableDependencyCount}
                    </strong>
                  </span>
                  <button
                    className="button button--secondary history-view"
                    onClick={() => onView(scan.id)}
                    disabled={busy}
                    aria-label={`Ver análisis ${scan.id} de ${scan.filename}`}
                    aria-current={selected ? "true" : undefined}
                  >
                    {openingId === scan.id ? (
                      <>
                        <span className="spinner" />
                        Abriendo…
                      </>
                    ) : (
                      <>
                        Ver análisis
                        <Icon name="arrow" />
                      </>
                    )}
                  </button>
                </li>
              );
            })}
          </ul>
        </>
      )}
      <p className="history-footnote">
        <Icon name="info" />
        Historial de demostración compartido entre los usuarios autenticados.
      </p>
    </section>
  );
}
