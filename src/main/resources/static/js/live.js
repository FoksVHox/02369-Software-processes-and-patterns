document.addEventListener("DOMContentLoaded", () => {
    const refreshFragments = async () => {
        await Promise.all([
            updateFragment("/live/songlist", "song-list-container"),
            updateFragment("/live/current", "current-playing-container")
        ]);
    };

    refreshFragments();
    setInterval(refreshFragments, 2000);
});

async function updateFragment(url, containerId) {
    try {
        const fragmentResp = await fetch(url, { headers: { "Accept": "text/html" } });
        if (!fragmentResp.ok) {
            console.warn("Fragment fetch failed:", fragmentResp.status, url);
            return;
        }
        const html = await fragmentResp.text();
        const container = document.getElementById(containerId);
        if (!container) {
            console.warn("Couldn't find #" + containerId + " in the DOM. Skipping update.");
            return;
        }
        container.innerHTML = html;
    } catch (err) {
        console.error("Error while fetching fragment", url, err);
    }
}
