function handleSubmit(event: any) {
    event.preventDefault();

    const apiUrl = `/api/v1/groups`;
    const token = localStorage.getItem("access_token");

    fetch(apiUrl, {
        method: 'POST',
        body: JSON.stringify(new CreateGroupRequest()),
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
    .then((res) => {
        const group = res.json();
        console.log(group);
    )
}