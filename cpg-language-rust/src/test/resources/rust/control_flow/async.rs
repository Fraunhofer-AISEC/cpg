async fn async_fn() {
    let x = 1;
}

async fn caller() {
    async_fn().await;
}
