Behaviour.specify('a[id$="-showlink"]', 'tap-result', 0, function(e) {
    const id = e.id.replace('-showlink', '');
    e.onclick = function() {
        const query = e.getAttribute('data-query');
        const element = document.getElementById(id);

        element.style.display = '';

        const showLink = document.getElementById(`${id}-showlink`);
        const hideLink = document.getElementById(`${id}-hidelink`);

        if (showLink) showLink.style.display = 'none';
        if (hideLink) hideLink.style.display = '';

        var rqo = new XMLHttpRequest();
        rqo.open('GET', query, true);
        rqo.onreadystatechange = function() { element.innerHTML = rqo.responseText; }
        rqo.send(null);
    };
});

Behaviour.specify('a[id$="-hidelink"]', 'tap-result', 0, function(e) {
    const id = e.id.replace('-hidelink', '');
    e.onclick = function() {
        const element = document.getElementById(id);
        const showLink = document.getElementById(`${id}-showlink`);
        const hideLink = document.getElementById(`${id}-hidelink`);

        element.style.display = 'none';
        showLink.style.display = '';
        hideLink.style.display = 'none';
    };
});
