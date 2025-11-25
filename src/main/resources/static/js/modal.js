// Generic modal/remote-content helper

(function () {
    function escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function show(el) { el && el.classList.remove('hidden'); }
    function hide(el) { el && el.classList.add('hidden'); }

    function initAjaxModal(opts = {}) {
        const {
            openBtnSelector,
            modalSelector,
            closeBtnSelector,
            bodySelector,
            loadingSelector,
            openUrlAttr = 'data-url',
            fetchOnOpen = true
        } = opts;

        const openBtn = document.querySelector(openBtnSelector);
        const modal = document.querySelector(modalSelector);
        const closeBtn = closeBtnSelector ? document.querySelector(closeBtnSelector) : modal && modal.querySelector('[data-modal-close]');
        const bodyContainer = bodySelector ? document.querySelector(bodySelector) : modal && modal.querySelector('.modal-body');
        const loadingIndicator = loadingSelector ? document.querySelector(loadingSelector) : null;

        if (!modal || !openBtn) return;

        function attachHandlers() {
            // GET form interception (search)
            const getForm = bodyContainer && (bodyContainer.querySelector('form[method="get"], form:not([method])'));
            if (getForm && !getForm.dataset._handledGet) {
                getForm.dataset._handledGet = '1';
                getForm.addEventListener('submit', (e) => {
                    e.preventDefault();
                    const actionAttr = getForm.getAttribute('action') ?? '';
                    const base = actionAttr === '' ? (openBtn.dataset?.url || '') : actionAttr;
                    const params = new URLSearchParams(new FormData(getForm)).toString();
                    const url = base + (params ? ('?' + params) : '');
                    show(loadingIndicator);
                    fetch(url, { credentials: 'same-origin' })
                        .then(async resp => {
                            const text = await resp.text();
                            if (!resp.ok) {
                                bodyContainer.innerHTML = `<div class="text-red-400 mb-4">Could not load search results (status ${resp.status}).</div><div class="prose max-w-none text-left">${escapeHtml(text)}</div>`;
                                hide(loadingIndicator);
                                return;
                            }
                            bodyContainer.innerHTML = text;
                            hide(loadingIndicator);
                            attachHandlers(); // reattach for new content
                        })
                        .catch(err => {
                            console.error('Search fetch error:', err);
                            bodyContainer.innerHTML = '<div class="text-red-500">Network error while searching. See console.</div>';
                            hide(loadingIndicator);
                        });
                });
            }

            // POST form interception (add)
            const postForms = bodyContainer ? bodyContainer.querySelectorAll('form[method="post"]') : [];
            postForms.forEach(form => {
                if (form.dataset._handledPost === '1') return;
                form.dataset._handledPost = '1';
                form.addEventListener('submit', (e) => {
                    e.preventDefault();
                    const actionAttr = form.getAttribute('action') || '';
                    const action = actionAttr === '' ? (openBtn.dataset?.url || '') : actionAttr;
                    const enctype = (form.enctype || 'application/x-www-form-urlencoded').toLowerCase();
                    const fetchOptions = { method: 'POST', credentials: 'same-origin', headers: {} };

                    if (enctype === 'multipart/form-data') {
                        fetchOptions.body = new FormData(form);
                    } else if (enctype === 'application/x-www-form-urlencoded') {
                        fetchOptions.body = new URLSearchParams(new FormData(form));
                        fetchOptions.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
                    } else {
                        fetchOptions.body = new FormData(form);
                    }

                    show(loadingIndicator);
                    fetch(action, fetchOptions)
                        .then(async resp => {
                            const text = await resp.text();
                            hide(loadingIndicator);
                            if (!resp.ok) {
                                console.error('Add-song failed:', resp.status, resp.url, text);
                                bodyContainer.innerHTML = `<div class="text-red-400 mb-4">Could not submit (status ${resp.status}). Server response below:</div><div class="prose max-w-none text-left">${escapeHtml(text)}</div>`;
                                return;
                            }
                            // on success: close modal and reload (preserves current app logic)
                            closeModal();
                            window.location.reload();
                        })
                        .catch(err => {
                            hide(loadingIndicator);
                            console.error('Submit network error:', err);
                            const errEl = document.createElement('div');
                            errEl.className = 'text-red-500 mt-2';
                            errEl.textContent = 'Network error while submitting.';
                            form.parentElement && form.parentElement.appendChild(errEl);
                        });
                });
            });
        }

        function openModal() {
            const url = openBtn.dataset?.url || '';
            modal.classList.remove('hidden');
            if (!fetchOnOpen || !bodyContainer) return;
            show(loadingIndicator);
            fetch(url, { credentials: 'same-origin' })
                .then(async resp => {
                    const text = await resp.text();
                    if (!resp.ok) {
                        console.error('Fetch failed:', { status: resp.status, url: resp.url, body: text });
                        bodyContainer.innerHTML = `<div class="text-red-400 mb-4">Could not load content (status ${resp.status}). Server response below:</div><div class="prose max-w-none text-left">${escapeHtml(text)}</div>`;
                        hide(loadingIndicator);
                        return;
                    }
                    bodyContainer.innerHTML = text;
                    hide(loadingIndicator);
                    attachHandlers();
                })
                .catch(err => {
                    console.error('Fetch exception:', err);
                    if (bodyContainer) bodyContainer.innerHTML = '<div class="text-red-500">Network error while loading content. See console.</div>';
                    hide(loadingIndicator);
                });
        }

        function closeModal() {
            modal.classList.add('hidden');
            if (bodyContainer) bodyContainer.innerHTML = '';
        }

        // Wire open/close
        if (openBtn) openBtn.addEventListener('click', openModal);
        if (closeBtn) closeBtn.addEventListener('click', closeModal);
        modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });

        // expose for debugging if needed
        return { openModal, closeModal, attachHandlers };
    }

    // Auto-init common modals if present
    document.addEventListener('DOMContentLoaded', () => {
        // Add-song modal used in vote.html (AJAX content load)
        initAjaxModal({
            openBtnSelector: '#openAddSongModal',
            modalSelector: '#addSongModal',
            closeBtnSelector: '#closeAddSongModal',
            bodySelector: '#addSongModalBody',
            loadingSelector: '#addSongLoading',
            openUrlAttr: 'data-url',
            fetchOnOpen: true
        });

        // Session modal used in dashboard.html (static content; no fetch required)
        initAjaxModal({
            openBtnSelector: '#openModal',
            modalSelector: '#sessionModal',
            closeBtnSelector: '#closeModal',
            bodySelector: null,
            loadingSelector: null,
            fetchOnOpen: false
        });
    });

    // expose initializer for manual use
    window.initAjaxModal = initAjaxModal;
})();