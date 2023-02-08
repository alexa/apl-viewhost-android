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
#if defined( __cpp_exceptions )
         const internal::file_opener fo( "Makefile" );
         ::close( fo.m_fd );  // Provoke exception, nobody would normally do this.
         try {
            (void)fo.size();
            std::cerr << "pegtl: unit test failed for [ internal::file_opener ] " << std::endl;
            ++failed;
         }
         catch( const std::exception& ) {
         }
#endif
      }

   }  // namespace TAO_PEGTL_NAMESPACE

}  // namespace tao

#include "main.hpp"

#endif
