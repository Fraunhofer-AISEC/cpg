interface LoginResponse {
    access_token: string;
}

export const LoginForm: React.FunctionComponent<{}> = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const history = useHistory();

    function validateForm() {
        return username.length > 0 && password.length > 0;
    }

    function handleSubmit(event: any) {
        alert('A name was submitted: ' + username);
        event.preventDefault();
        const apiUrl = `/auth/login`;

        fetch(apiUrl, {
            method: 'POST', body: JSON.stringify({
                "username": username,
                "password": password
            })
        })
            .then((res) => res.json())
            .then((response: LoginResponse) => {
                history.push("/");
                localStorage.setItem("access_token", response.access_token);
            });
    }

    return (
        <Form onSubmit={handleSubmit}>
            <Form.Group controlId="email">
                <Form.Label>Email</Form.Label>
                <Form.Control
                    autoFocus
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                />
            </Form.Group>
            <Form.Group controlId="password">
                <Form.Label>Password</Form.Label>
                <Form.Control
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                />
            </Form.Group>
            <Button block size="lg" type="submit" disabled={!validateForm()}>
                Login
        </Button>
        </Form>
    );
}