#include <iostream>
#include <string>

template<typename T, int N=10>
class Array
{
private:
	T m_Array[N];
public:
	int GetSize() const { return N; }

};


int main()
{
	Array<int> array;
	std::cout << array.GetSize() << std::endl;

	std::cin.get();


}