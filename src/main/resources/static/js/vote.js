document.addEventListener("DOMContentLoaded", () => {
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

            // fetch updated fragment
            const fragmentResp = await fetch("/vote/songlist", { headers: { "Accept": "text/html" } });
            if (!fragmentResp.ok) {
                console.warn("Fragment fetch failed:", fragmentResp.status);
                return;
            }
            const html = await fragmentResp.text();

            // update container only if it exists
            const container = document.getElementById("song-list-container");
            if (!container) {
                console.warn("Couldn't find #song-list-container in the DOM. Skipping update.");
                return;
            }
            container.innerHTML = html;
        } catch (err) {
            console.error("Error while submitting vote / updating fragment:", err);
        }
    });
});