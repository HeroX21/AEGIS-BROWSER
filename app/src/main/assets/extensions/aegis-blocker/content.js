// Aegis Blocker - Cosmetic Filter & Element Hiding Content Script
(function() {
  const cosmeticSelectors = [
    ".ad-container",
    ".ad-wrapper",
    ".adsbox",
    ".ad-banner",
    ".advertisement",
    ".google-auto-placed",
    ".sponsor-post",
    "#ad-slot",
    "#ad-banner",
    "#div-gpt-ad",
    "#sponsored-content"
  ];

  function hideElements() {
    const styleId = "aegis-cosmetic-styles";
    if (!document.getElementById(styleId) && document.head) {
      const style = document.createElement("style");
      style.id = styleId;
      style.textContent = cosmeticSelectors.join(", ") + " { display: none !important; visibility: hidden !important; height: 0 !important; }";
      document.head.appendChild(style);
    }
  }

  // Inject at document start
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", hideElements);
  } else {
    hideElements();
  }
})();
