export interface User {
    id: number
    username: string
    firstName: string
    lastName: string
}

export interface UsersState {
    users: User[];
}

/* Comment on a record */
export class Users extends React.Component<{}, UsersState> {

    // Comment on constructor
    constructor(props: {}) {
        super(props);

        this.state = {
            users: []
        }
    }

    /*
        Multiline comment inside of a file
    */
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