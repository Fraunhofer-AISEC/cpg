pub struct Container<T> {
    pub data: Vec<T>,
    count: usize,
}

impl<T> Container<T> {
    pub fn new() -> Self {
        Container { data: Vec::new(), count: 0 }
    }

    pub fn add(&mut self, item: T) {
        self.data.push(item);
        self.count += 1;
    }

    fn len(&self) -> usize {
        self.count
    }
}

fn test_multi_generic<A, B>(a: A, b: B) -> (A, B) {
    (a, b)
}

