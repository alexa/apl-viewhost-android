// Copyright (c) 2014-2020 Dr. Colin Hirsch and Daniel Frey
// Please see LICENSE for license or visit https://github.com/taocpp/PEGTL/

#include <tao/pegtl.hpp>

#include "json_errors.hpp"

using namespace tao::TAO_PEGTL_NAMESPACE;  // NOLINT
using grammar = seq< json::text, eof >;

int main( int argc, char** argv )  // NOLINT
{
   for( int i = 1; i < argc; ++i ) {
      argv_input<> in( argv, i );
#if defined( __cpp_exceptions )
      parse< grammar, nothing, examples::errors >( in );
#else
      if( !parse< grammar, nothing, examples::errors >( in ) ) {
         std::perror("error occurred");
         return 1;
      }
#endif
   }
   return 0;
}
