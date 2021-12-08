export interface User {
    id: number
    username: string
    firstName: string
    lastName: string
}

export interface UsersState {
    users: User[];
}

export class Users extends React.Component<{}, UsersState> {

    constructor(props: {}) {
        super(props);

        this.state = {
            users: []
        }
    }

    public componentDidMount() {

    }

    public render() {
        const { users } = this.state;

        return <div>
            {users.map((user: User) =>
                <div key={user.username}>{user.username}</div>
            )}
        </div>
    }
}