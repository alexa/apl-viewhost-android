// Copyright (c) 2015-2020 Dr. Colin Hirsch and Daniel Frey
// Please see LICENSE for license or visit https://github.com/taocpp/PEGTL/

#if !defined( __cpp_exceptions ) || !defined( _POSIX_MAPPED_FILES )
int main() {}
#else

#include <tao/pegtl/file_input.hpp>

#include "test.hpp"

namespace tao
{
   namespace TAO_PEGTL_NAMESPACE
   {
      void unit_test()
      {
         try {
            internal::file_mapper dummy( "include" );
            std::cerr << "pegtl: unit test failed for [ internal::file_mapper ]" << std::endl;
            ++failed;
         }
         catch( const input_error& ) {
         }
         catch( ... ) {
            std::cerr << "pegtl: unit test failed for [ internal::file_mapper ] with unexpected exception" << std::endl;
            ++failed;
         }
      }

   }  // namespace TAO_PEGTL_NAMESPACE

}  // namespace tao

#include "main.hpp"

#endif
