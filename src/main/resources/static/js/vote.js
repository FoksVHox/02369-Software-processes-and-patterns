document.addEventListener("DOMContentLoaded", () => {
    const refreshFragments = async () => {
        await Promise.all([
            updateFragment("/vote/songlist", "song-list-container"),
            updateFragment("/vote/current", "current-playing-container")
        ]);
    };

    document.body.addEventListener("submit", async (e) => {
        try {
            if (!e.target.classList.contains("vote-form")) return;

            e.preventDefault(); // stop full refresh

            const form = e.target;
            const formData = new FormData(form);

            const submitter = e.submitter;
            if (submitter && submitter.name && submitter.value) {
                formData.append(submitter.name, submitter.value);
            }

            // send the vote
            const voteResp = await fetch(form.action, { method: "POST", body: formData });
            if (!voteResp.ok) {
                console.warn("Vote POST failed:", voteResp.status);
            }

            await refreshFragments();
        } catch (err) {
            console.error("Error while submitting vote / updating fragment:", err);
        }
    });

    refreshFragments();
    setInterval(refreshFragments, 5000);
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
