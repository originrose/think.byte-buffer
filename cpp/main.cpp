#include "byte_buffer_export.hpp"


int main( int c, char** v)
{
  using namespace think::byte_buffer;
  BufferManager* manager = BufferManager::create_buffer_manager();
  manager->release_manager();
  return 0;
}
