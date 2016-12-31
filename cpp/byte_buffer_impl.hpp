#ifndef BYTE_BUFFER_IMPL_HPP
#define BYTE_BUFFER_IMPL_HPP
#include <memory>
#include <stdexcept>
#include <cstring>
#include "byte_buffer.hpp"

namespace think { namespace byte_buffer {
    using namespace std;

    template<Datatype::Enum>
    struct datatype_to_type
    {
    };

    template<typename type>
    struct type_to_datatype
    {
    };


#define DEFINE_DATATYPE(dtype,type) \
    template<> struct datatype_to_type<Datatype::dtype> { typedef type TType; }; \
    template<> struct type_to_datatype<type> { static Datatype::Enum datatype() { return Datatype::dtype; } };

    DEFINE_DATATYPE(Byte,char);
    DEFINE_DATATYPE(Short,short);
    DEFINE_DATATYPE(Int,int32_t);
    DEFINE_DATATYPE(Long,int64_t);
    DEFINE_DATATYPE(Float,float);
    DEFINE_DATATYPE(Double,double);


    template<typename lhs, typename rhs>
    struct copy_op
    {
      static inline void copy(const lhs* src, int64_t src_offset,
			      rhs* dst, int64_t dst_offset,
			      int64_t n_elems)
      {
	dst += dst_offset;
	src += src_offset;
	for(int64_t idx = 0; idx < n_elems; ++idx) {
	  dst[idx] = (rhs) src[idx];
	}
      }
    };

    template<typename dtype>
    struct single_copy_op
    {
      static inline void copy( const dtype* src, int64_t src_offset,
			       dtype* dst, int64_t dst_offset,
			       int64_t n_elems )
      {
	memcpy((char*) (dst + dst_offset),
	       (const char*) (src + src_offset),
	       n_elems * sizeof(dtype));
      }
    };

#define DEFINE_SINGLE_TYPE_COPY(dtype) \
    template<> struct copy_op<dtype,dtype> { \
      static inline void copy(const dtype* src, int64_t src_offset, \
			      dtype * dst, int64_t dst_offset, \
			      int64_t n_elems) { \
	single_copy_op<dtype>::copy( src, src_offset, dst, dst_offset, n_elems);}};


    DEFINE_SINGLE_TYPE_COPY(char);
    DEFINE_SINGLE_TYPE_COPY(short);
    DEFINE_SINGLE_TYPE_COPY(int32_t);
    DEFINE_SINGLE_TYPE_COPY(int64_t);
    DEFINE_SINGLE_TYPE_COPY(float);
    DEFINE_SINGLE_TYPE_COPY(double);


    template<typename src_type, typename dst_type>
    inline void do_copy(const src_type* src, int64_t src_offset,
			dst_type* dst, int64_t dst_offset,
			int64_t n_elems){
      copy_op<src_type,dst_type>::copy(src, src_offset, dst, dst_offset, n_elems);
    }


    template<typename lhs, typename rhs>
    struct access
    {
      static inline void set(lhs* dst, int64_t dst_offset, rhs src, int64_t n_elems)
      {
	dst += dst_offset;
	for( int64_t idx = 0; idx < n_elems; ++idx ) dst[idx] = (lhs)src;
      }
      static inline rhs get(const lhs* src, int64_t src_offset)
      {
	return static_cast<rhs>(src[src_offset]);
      }
    };

    template<typename dst_type, typename src_type>
    inline dst_type do_get(const src_type* src, int64_t src_offset ) {
      return access<src_type,dst_type>::get(src, src_offset);
    }

    template<typename src_type, typename dst_type>
    inline void do_set(src_type* src, int64_t src_offset, dst_type value, int64_t n_elems ) {
      access<src_type,dst_type>::set(src, src_offset, value, n_elems);
    }


    template<typename TRetType, typename TOpType>
    inline TRetType typed_buffer_op(TypedBuffer buffer, TOpType op)
    {
      switch(buffer.m_datatype) {
      case Datatype::Byte: return op((typename datatype_to_type<Datatype::Byte>::TType*)buffer.m_buffer);
      case Datatype::Short: return op((typename datatype_to_type<Datatype::Short>::TType*)buffer.m_buffer);
      case Datatype::Int: return op((typename datatype_to_type<Datatype::Int>::TType*)buffer.m_buffer);
      case Datatype::Long: return op((typename datatype_to_type<Datatype::Long>::TType*)buffer.m_buffer);
      case Datatype::Float: return op((typename datatype_to_type<Datatype::Float>::TType*)buffer.m_buffer);
      case Datatype::Double: return op((typename datatype_to_type<Datatype::Double>::TType*)buffer.m_buffer);
      };
      throw exception();
      return TRetType();
    }

    struct BufferManagerImpl : public BufferManager
    {
      BufferManagerImpl(){}
      virtual ~BufferManagerImpl(){}
      virtual TypedBuffer allocate( int64_t size, const char* file, int line )
      {
	return TypedBuffer(reinterpret_cast<int64_t>(malloc(size)), size, Datatype::Byte);
      }
      virtual void release( int64_t data)
      {
	free((void*)data);
      }

      virtual void copy( TypedBuffer src, int64_t src_offset,
			 char* dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(src, [=](auto src_ptr) {
	    do_copy(src_ptr, src_offset, dst, dst_offset, n_elems); });
      }
      virtual void copy( TypedBuffer src, int64_t src_offset,
			 short* dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(src, [=](auto src_ptr) { do_copy(src_ptr, src_offset,
							       dst, dst_offset, n_elems); });
      }
      virtual void copy( TypedBuffer src, int64_t src_offset,
			 int32_t* dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(src, [=](auto src_ptr) { do_copy(src_ptr, src_offset,
							       dst, dst_offset, n_elems); });
      }
      virtual void copy( TypedBuffer src, int64_t src_offset,
			 int64_t* dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(src, [=](auto src_ptr) { do_copy(src_ptr, src_offset,
							       dst, dst_offset, n_elems); });
      }
      virtual void copy( TypedBuffer src, int64_t src_offset, float* dst,
			 int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(src, [=](auto src_ptr) { do_copy(src_ptr, src_offset,
							       dst, dst_offset, n_elems); });
      }
      virtual void copy( TypedBuffer src, int64_t src_offset,
			 double* dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(src, [=](auto src_ptr) { do_copy(src_ptr, src_offset,
							       dst, dst_offset, n_elems); });
      }


      virtual void copy( const char* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_copy(src, src_offset,
							       dst_ptr, dst_offset, n_elems); });
      }
      virtual void copy( const short* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_copy(src, src_offset,
							       dst_ptr, dst_offset, n_elems); });
      }
      virtual void copy( const int32_t* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_copy(src, src_offset,
							       dst_ptr, dst_offset, n_elems); });
      }
      virtual void copy( const int64_t* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_copy(src, src_offset,
							       dst_ptr, dst_offset, n_elems); });
      }
      virtual void copy( const float* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_copy(src, src_offset,
							       dst_ptr, dst_offset, n_elems); });
      }
      virtual void copy( const double* src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_copy(src, src_offset,
							       dst_ptr, dst_offset, n_elems); });
      }


      virtual void copy( TypedBuffer src, int64_t src_offset,
			 TypedBuffer dst, int64_t dst_offset, int64_t n_elems ) {
	typed_buffer_op<void>(src, [=](auto src_ptr) {
	    typed_buffer_op<void>(dst, [=](auto dst_ptr) {
		do_copy(src_ptr, src_offset,
			dst_ptr, dst_offset, n_elems);
	      } );
	  } );
      }


      virtual void set_value( TypedBuffer dst, int64_t offset, char value, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_set(dst_ptr, offset, value, n_elems); });
      }
      virtual void set_value( TypedBuffer dst, int64_t offset, short value, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_set(dst_ptr, offset, value, n_elems); });
      }
      virtual void set_value( TypedBuffer dst, int64_t offset, int32_t value, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_set(dst_ptr, offset, value, n_elems); });
      }
      virtual void set_value( TypedBuffer dst, int64_t offset, int64_t value, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_set(dst_ptr, offset, value, n_elems); });
      }
      virtual void set_value( TypedBuffer dst, int64_t offset, float value, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_set(dst_ptr, offset, value, n_elems); });
      }
      virtual void set_value( TypedBuffer dst, int64_t offset, double value, int64_t n_elems ) {
	typed_buffer_op<void>(dst, [=](auto dst_ptr) { do_set(dst_ptr, offset, value, n_elems); });
      }

      virtual char get_value_char( TypedBuffer src, int64_t offset ) {
	return typed_buffer_op<char>(src, [=](auto src_ptr) { return do_get<char>(src_ptr, offset); });
      }
      virtual short get_value_short( TypedBuffer src, int64_t offset ) {
	return typed_buffer_op<short>(src, [=](auto src_ptr) { return do_get<short>(src_ptr, offset); });
      }
      virtual int32_t get_value_int32_t( TypedBuffer src, int64_t offset ) {
	return typed_buffer_op<int32_t>(src, [=](auto src_ptr) { return do_get<int32_t>(src_ptr, offset); });
      }
      virtual int64_t get_value_int64( TypedBuffer src, int64_t offset ) {
	return typed_buffer_op<int64_t>(src, [=](auto src_ptr) { return do_get<int64_t>(src_ptr, offset); });
      }
      virtual float get_value_float( TypedBuffer src, int64_t offset ) {
	return typed_buffer_op<float>(src, [=](auto src_ptr) { return do_get<float>(src_ptr, offset); });
      }
      virtual double get_value_double( TypedBuffer src, int64_t offset ) {
	return typed_buffer_op<double>(src, [=](auto src_ptr) { return do_get<double>(src_ptr, offset); });
      }
      virtual void release_manager() {
	delete this;
      }
    };
  }
}
#endif
