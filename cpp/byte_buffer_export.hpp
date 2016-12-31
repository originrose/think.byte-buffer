#ifndef BYTE_BUFFER_EXPORT_HPP
#define BYTE_BUFFER_EXPORT_HPP
#include "byte_buffer_impl.hpp"

namespace think { namespace byte_buffer {
    BufferManager* BufferManager::create_buffer_manager()
    {
      return new BufferManagerImpl();
    }
  }}

#endif
