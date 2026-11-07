import { Icon } from "./Workspace";

function fixedVersions(vulnerability, dependency) {
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

function safeUrl(value) {
  try {
    const url = new URL(value);
    return ["http:", "https:"].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}

function VulnerabilityCard({ vulnerability, dependency }) {
  const severity = vulnerability.database_specific?.severity;
  const levels = {
    CRITICAL: ["Crítica", "critical"],
    HIGH: ["Alta", "high"],
    MODERATE: ["Moderada", "moderate"],
    MEDIUM: ["Media", "moderate"],
    LOW: ["Baja", "low"],
  };
  const [severityLabel, severityClass] = levels[severity] ?? [
    "No disponible",
    "unknown",
  ];
  const identifiers = [
    ...new Set(
      [vulnerability.id, ...(vulnerability.aliases ?? [])].filter(Boolean),
    ),
  ];
  const cves = identifiers.filter((id) => id.startsWith("CVE-"));
  const ghsas = identifiers.filter(
    (id) => id.startsWith("GHSA-") && id !== vulnerability.id,
  );
  const versions = fixedVersions(vulnerability, dependency);
  const details = vulnerability.details?.trim();
  const description =
    vulnerability.summary ||
    (details && details.length > 320 ? `${details.slice(0, 320)}…` : details) ||
    "No disponible";
  const references = vulnerability.references ?? [];
  const primaryReference = references.find((reference) =>
    safeUrl(reference.url),
  );
  const hasMoreDescription = details && details !== description;
  return (
    <article
      className="vulnerability-card"
      aria-label={`Vulnerabilidad ${vulnerability.id ?? "sin identificador"}`}
    >
      <div className="vulnerability-heading">
        <h4>{vulnerability.id ?? "Identificador no disponible"}</h4>
        <span className={`badge badge--${severityClass}`}>
          Gravedad: {severityLabel}
        </span>
      </div>
      <p className="identifiers">
        <span className="meta-label">CVE</span>
        {cves.length ? (
          cves.map((cve) => (
            <code className="identifier" key={cve}>
              {cve}
            </code>
          ))
        ) : (
          <span>No disponible</span>
        )}
        {ghsas.map((id) => (
          <code className="identifier" key={id}>
            {id}
          </code>
        ))}
      </p>
      <p className="vulnerability-description">{description}</p>
      {hasMoreDescription && (
        <details className="description-details">
          <summary>Ver descripción completa</summary>
          <p className="long-description">{details}</p>
        </details>
      )}
      <div className="vulnerability-footer">
        <div className="fixed-versions">
          <Icon name="layers" />
          <div>
            <span className="meta-label">Versiones corregidas según OSV</span>
            <span>
              {versions.length
                ? versions.map((version) => (
                    <code key={version}>{version}</code>
                  ))
                : "No disponibles"}
            </span>
          </div>
        </div>
        {primaryReference && (
          <a
            className="text-link"
            href={safeUrl(primaryReference.url)}
            target="_blank"
            rel="noopener noreferrer"
          >
            Consultar aviso <Icon name="link" />
            <span className="visually-hidden">
              (se abre en una nueva pestaña)
            </span>
          </a>
        )}
      </div>
      <details className="technical-details">
        <summary>
          Datos técnicos y referencias
          {references.length ? ` (${references.length})` : ""}
        </summary>
        <div className="technical-content">
          <p>
            <strong>Identificadores: </strong>
            {identifiers.join(", ") || "No disponibles"}
          </p>
          <div>
            <strong>Datos de gravedad de OSV: </strong>
            {vulnerability.severity?.length ? (
              <ul>
                {vulnerability.severity.map((item, index) => (
                  <li key={index}>
                    <code>
                      {item.type}: {item.score}
                    </code>
                  </li>
                ))}
              </ul>
            ) : (
              <span>No disponibles</span>
            )}
          </div>
          <div>
            <strong>Referencias: </strong>
            {references.length ? (
              <ul className="reference-list">
                {references.map((reference, index) => (
                  <li key={index}>
                    {safeUrl(reference.url) ? (
                      <a
                        href={safeUrl(reference.url)}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {reference.type ? `${reference.type} · ` : ""}
                        {reference.url}
                        <span className="visually-hidden">
                          {" "}
                          (se abre en una nueva pestaña)
                        </span>
                      </a>
                    ) : (
                      <span>{reference.url || "Enlace no disponible"}</span>
                    )}
                  </li>
                ))}
              </ul>
            ) : (
              <span>No disponibles</span>
            )}
          </div>
        </div>
      </details>
    </article>
  );
}

export default function ScanResults({
  result,
  metadata,
  loading,
  openingId,
  error,
  headingRef,
}) {
  const results = result?.results ?? [];
  const vulnerableCount = results.filter(
    (dependency) => dependency.vulnerabilities?.length > 0,
  ).length;
  const busy = loading || openingId !== null;
  return (
    <section
      className="panel results-panel"
      id="resultados"
      aria-labelledby="results-title"
      aria-busy={busy}
    >
      <div className="section-heading">
        <div className="section-title">
          <span className="step-number" aria-hidden="true">
            02
          </span>
          <div>
            <h2 id="results-title" ref={headingRef} tabIndex={-1}>
              Resultados del análisis
            </h2>
            <p>
              {result
                ? `${metadata?.filename || "pom.xml"} · Análisis #${result.scanId}`
                : "Una visión clara de las dependencias de tu proyecto."}
            </p>
          </div>
        </div>
        {result && (
          <span className="saved-label">
            <Icon name="check" />
            Guardado en el historial
          </span>
        )}
      </div>
      {error && (
        <div className="notice notice--error" role="alert">
          <Icon name="info" />
          <p>{error}</p>
        </div>
      )}
      {busy ? (
        <div className="empty-state" role="status">
          <span className="spinner spinner--large" />
          <h3>
            {loading ? "Revisando tus dependencias" : "Recuperando el análisis"}
          </h3>
          <p>
            {loading
              ? "Estamos consultando las vulnerabilidades conocidas en OSV."
              : "Cargando los resultados guardados en el historial."}
          </p>
        </div>
      ) : !result ? (
        <div className="empty-state">
          <span className="empty-icon">
            <Icon name="shield" />
          </span>
          <h3>
            {error
              ? "No se ha podido completar la solicitud"
              : "Tus resultados aparecerán aquí"}
          </h3>
          <p>
            {error
              ? "Revisa el mensaje e inténtalo de nuevo."
              : "Sube un pom.xml o abre un análisis del historial para ver sus dependencias y avisos de seguridad."}
          </p>
        </div>
      ) : (
        <>
          <p className="results-summary" role="status">
            <span>
              <Icon name="layers" />
              <strong>{results.length}</strong>{" "}
              {results.length === 1 ? "dependencia" : "dependencias"}
            </span>
            <span className="summary-divider" aria-hidden="true" />
            <span>
              <strong>{vulnerableCount}</strong> con vulnerabilidades conocidas
            </span>
          </p>
          {results.length === 0 ? (
            <div className="empty-state compact">
              <span className="empty-icon">
                <Icon name="file" />
              </span>
              <h3>Sin dependencias directas que mostrar</h3>
              <p>
                El análisis se ha guardado, pero no contiene dependencias
                directas.
              </p>
            </div>
          ) : (
            <div className="dependency-list">
              {results.map((dependency, index) => {
                const vulnerabilities = dependency.vulnerabilities ?? [];
                const skipped =
                  dependency.status === "SKIPPED_UNRESOLVED_VERSION";
                return (
                  <article
                    className="dependency-result"
                    key={`${dependency.groupId}:${dependency.artifactId}:${index}`}
                  >
                    <div className="dependency-heading">
                      <div className="dependency-identity">
                        <span className="dependency-icon">
                          <Icon name="layers" />
                        </span>
                        <div>
                          <h3>{dependency.artifactId}</h3>
                          <p>{dependency.groupId}</p>
                        </div>
                      </div>
                      <div className="dependency-badges">
                        <span className="tag version-tag">
                          Versión{" "}
                          <code>{dependency.version || "No especificada"}</code>
                        </span>
                        <span
                          className={`badge badge--${skipped ? "moderate" : vulnerabilities.length ? "risk" : "clean"}`}
                        >
                          {skipped
                            ? "Sin analizar"
                            : vulnerabilities.length
                              ? `${vulnerabilities.length} ${vulnerabilities.length === 1 ? "vulnerabilidad" : "vulnerabilidades"}`
                              : "Sin vulnerabilidades conocidas"}
                        </span>
                      </div>
                    </div>
                    {skipped ? (
                      <p className="dependency-note">
                        <Icon name="info" />
                        No se pudo resolver la versión. Esta dependencia se
                        conserva en el historial, pero no se ha analizado.
                      </p>
                    ) : vulnerabilities.length ? (
                      <div className="vulnerability-list">
                        {vulnerabilities.map((vulnerability, i) => (
                          <VulnerabilityCard
                            vulnerability={vulnerability}
                            dependency={dependency}
                            key={vulnerability.id ?? i}
                          />
                        ))}
                      </div>
                    ) : (
                      <p className="dependency-note">
                        <Icon name="check" />
                        No se encontraron vulnerabilidades conocidas para esta
                        versión.
                      </p>
                    )}
                  </article>
                );
              })}
            </div>
          )}
          <p className="results-footnote">
            Los resultados reflejan la información conocida por OSV en el
            momento del análisis.
          </p>
        </>
      )}
    </section>
  );
}
