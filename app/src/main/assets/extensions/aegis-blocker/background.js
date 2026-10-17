// Aegis Blocker - WebExtension Background Engine
let isBlockingEnabled = true;
let siteExceptions = new Set();
let nativePort = null;

// Connect to native GeckoView app via WebExtension messaging port
try {
  nativePort = browser.runtime.connectNative("browser");
  nativePort.onMessage.addListener((message) => {
    if (message.type === "UPDATE_CONFIG") {
      isBlockingEnabled = message.blockingEnabled !== false;
      if (Array.isArray(message.exceptions)) {
        siteExceptions = new Set(message.exceptions.map(e => e.toLowerCase()));
      }
    }
  });
} catch (e) {
  // Native port initialization fallback
}

// Built-in pattern rules
const blockedDomainPatterns = [
  "doubleclick.net", "googlesyndication.com", "googleadservices.com",
  "adnxs.com", "adsrvr.org", "advertising.com", "criteo.com", "criteo.net",
  "taboola.com", "outbrain.com", "moatads.com", "scorecardresearch.com",
  "quantserve.com", "amazon-adsystem.com", "rubiconproject.com",
  "pubmatic.com", "openx.net", "casalemedia.com", "adroll.com",
  "adcolony.com", "unityads.unity3d.com", "applovin.com", "vungle.com",
  "inmobi.com", "chartboost.com", "popads.net", "popcash.net",
  "propellerads.com", "exoclick.com", "adcash.com", "zeroredirect.com",
  "hotjar.com", "clarity.ms", "mixpanel.com", "segment.io"
];

function getHostName(url) {
  try {
    return new URL(url).hostname.toLowerCase();
  } catch (e) {
    return "";
  }
}

function isDomainBlocked(targetHost, sourceHost) {
  // Check if source site is in exception whitelist
  if (sourceHost && siteExceptions.has(sourceHost)) {
    return false;
  }
  for (const blocked of blockedDomainPatterns) {
    if (targetHost === blocked || targetHost.endsWith("." + blocked)) {
      return true;
    }
  }
  return false;
}

// Intercept HTTP/HTTPS web requests before network transmission
browser.webRequest.onBeforeRequest.addListener(
  function(details) {
    if (!isBlockingEnabled) {
      return { cancel: false };
    }

    const targetHost = getHostName(details.url);
    const sourceHost = details.documentUrl ? getHostName(details.documentUrl) : "";

    if (isDomainBlocked(targetHost, sourceHost)) {
      // Notify native app of blocked request
      try {
        if (nativePort) {
          nativePort.postMessage({
            type: "REQUEST_BLOCKED",
            url: details.url,
            domain: targetHost,
            sourceDomain: sourceHost,
            resourceType: details.type || "other",
            timestamp: Date.now()
          });
        }
      } catch (err) {
        // Port message dispatch error handled safely
      }
      return { cancel: true };
    }

    return { cancel: false };
  },
  { urls: ["<all_urls>"] },
  ["blocking"]
);
