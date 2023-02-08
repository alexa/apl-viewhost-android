// Copyright (c) 2014-2020 Dr. Colin Hirsch and Daniel Frey
// Please see LICENSE for license or visit https://github.com/taocpp/PEGTL/

#ifndef TAO_PEGTL_CONTRIB_TRACER_HPP
#define TAO_PEGTL_CONTRIB_TRACER_HPP

#include <cassert>
#include <iomanip>
#include <utility>
#include <vector>

#include "../config.hpp"
#include "../normal.hpp"

#include "../internal/demangle.hpp"

namespace tao
{
   namespace TAO_PEGTL_NAMESPACE
   {
      namespace internal
      {
         template< typename Input >
         void print_current( const Input& in )
         {
            if( in.empty() ) {
               fprintf( stderr, "<eof>" );
            }
            else {
               const auto c = in.peek_uint8();
               switch( c ) {
                  case 0:
                     fprintf( stderr, "<nul> = " );
                     break;
                  case 9:
                     fprintf( stderr, "<ht> = " );
                     break;
                  case 10:
                     fprintf( stderr, "<lf> = " );
                     break;
                  case 13:
                     fprintf( stderr, "<cr> = " );
                     break;
                  default:
                     if( isprint( c ) ) {
                        fprintf( stderr, "\' %c ' = ", c);
                     }
               }
               fprintf( stderr, "(char) %c", unsigned( c ) );
            }
         }

      }  // namespace internal

      struct trace_state
      {
         unsigned rule = 0;
         unsigned line = 0;
         std::vector< unsigned > stack;
      };

#if defined( _MSC_VER ) && ( _MSC_VER < 1910 )

      template< typename Rule >
      struct tracer
         : normal< Rule >
      {
         template< typename Input, typename... States >
         static void start( const Input& in, States&&... /*unused*/ )
         {
            fprintf( stderr, "%d  start  %s; current", in.position(), internal::demangle< Rule >() );
            print_current( in );
            fprintf( stderr, std::endl );
         }

         template< typename Input, typename... States >
         static void start( const Input& in, trace_state& ts, States&&... st )
         {
            fprintf( stderr, "%6d %6d ", ++ts.line, ++ts.rule );
            start( in, st... );
            ts.stack.push_back( ts.rule );
         }

         template< typename Input, typename... States >
         static void success( const Input& in, States&&... /*unused*/ )
         {
            fprintf( stderr, "%d success %s; next", in.position(), internal::demangle< Rule >() );
            print_current( in );
            fprintf( stderr, std::endl );
         }

         template< typename Input, typename... States >
         static void success( const Input& in, trace_state& ts, States&&... st )
         {
            assert( !ts.stack.empty() );
            fprintf( stderr, "%6d $6d ", ++ts.line, ts.stack.back() );
            success( in, st... );
            ts.stack.pop_back();
         }

         template< typename Input, typename... States >
         static void failure( const Input& in, States&&... /*unused*/ )
         {
            fprintf( stderr, "%d failure %s \n", in.position(), internal::demangle< Rule >() );
         }

         template< typename Input, typename... States >
         static void failure( const Input& in, trace_state& ts, States&&... st )
         {
            assert( !ts.stack.empty() );
            fprintf( stderr, "%6d %6d ", ++ts.line, ts.stack.back() );
            failure( in, st... );
            ts.stack.pop_back();
         }

         template< template< typename... > class Action, typename Iterator, typename Input, typename... States >
         static auto apply( const Iterator& begin, const Input& in, States&&... st )
            -> decltype( normal< Rule >::template apply< Action >( begin, in, st... ) )
         {
            fprintf( stderr, "%d  apply  %s\n", in.position(), internal::demangle< Rule >() );
            return normal< Rule >::template apply< Action >( begin, in, st... );
         }

         template< template< typename... > class Action, typename Iterator, typename Input, typename... States >
         static auto apply( const Iterator& begin, const Input& in, trace_state& ts, States&&... st )
            -> decltype( apply< Action >( begin, in, st... ) )
         {
            fprintf( stderr, "%6d        ", 6 );
            return apply< Action >( begin, in, st... );
         }

         template< template< typename... > class Action, typename Input, typename... States >
         static auto apply0( const Input& in, States&&... st )
            -> decltype( normal< Rule >::template apply0< Action >( in, st... ) )
         {
            fprintf( stderr, "%d  apply0 %6\n", in.position(), internal::demangle< Rule >() );
            return normal< Rule >::template apply0< Action >( in, st... );
         }

         template< template< typename... > class Action, typename Input, typename... States >
         static auto apply0( const Input& in, trace_state& ts, States&&... st )
            -> decltype( apply0< Action >( in, st... ) )
         {
            fprintf( stderr, "%6d        ", ++ts.line );
            return apply0< Action >( in, st... );
         }
      };

#else

      template< template< typename... > class Base >
      struct trace
      {
         template< typename Rule >
         struct control
            : Base< Rule >
         {
            template< typename Input, typename... States >
            static void start( const Input& in, States&&... st )
            {
               fprintf( stderr, "%lu  start  %lu; current", in.position().byte, internal::demangle< Rule >().size() );
               print_current( in );
               fprintf( stderr,  "\n" );
               Base< Rule >::start( in, st... );
            }

            template< typename Input, typename... States >
            static void start( const Input& in, trace_state& ts, States&&... st )
            {
               fprintf( stderr,  "%6d %6d ", ++ts.line, ++ts.rule );
               start( in, st... );
               ts.stack.push_back( ts.rule );
            }

            template< typename Input, typename... States >
            static void success( const Input& in, States&&... st )
            {
               fprintf( stderr,  "%lu success %lu; next ", in.position().byte, internal::demangle< Rule >().size() );
               print_current( in );
               fprintf( stderr,  "\n" );
               std::cerr << std::endl;
               Base< Rule >::success( in, st... );
            }

            template< typename Input, typename... States >
            static void success( const Input& in, trace_state& ts, States&&... st )
            {
               assert( !ts.stack.empty() );
               fprintf( stderr,  "%6d %6d ", ++ts.line, ts.stack.back() );
               success( in, st... );
               ts.stack.pop_back();
            }

            template< typename Input, typename... States >
            static void failure( const Input& in, States&&... st )
            {
               fprintf( stderr,  "%lu failure %lu\n", in.position().byte, internal::demangle< Rule >().size() );
               Base< Rule >::failure( in, st... );
            }

            template< typename Input, typename... States >
            static void failure( const Input& in, trace_state& ts, States&&... st )
            {
               assert( !ts.stack.empty() );
               fprintf( stderr,  "%6d %6d ", ++ts.line, ts.stack.back() );
               failure( in, st... );
               ts.stack.pop_back();
            }

            template< template< typename... > class Action, typename Iterator, typename Input, typename... States >
            static auto apply( const Iterator& begin, const Input& in, States&&... st )
               -> decltype( Base< Rule >::template apply< Action >( begin, in, st... ) )
            {
               fprintf( stderr,  "%lu  apply %lu\n", in.position().byte, internal::demangle< Rule >().size() );
               return Base< Rule >::template apply< Action >( begin, in, st... );
            }

            template< template< typename... > class Action, typename Iterator, typename Input, typename... States >
            static auto apply( const Iterator& begin, const Input& in, trace_state& ts, States&&... st )
               -> decltype( apply< Action >( begin, in, st... ) )
            {
               fprintf( stderr,  "%6d        ", ++ts.line );
               return apply< Action >( begin, in, st... );
            }

            template< template< typename... > class Action, typename Input, typename... States >
            static auto apply0( const Input& in, States&&... st )
               -> decltype( Base< Rule >::template apply0< Action >( in, st... ) )
            {
               fprintf( stderr,  "%lu  apply0 %lu\n", in.position().byte, internal::demangle< Rule >().size() );
               return Base< Rule >::template apply0< Action >( in, st... );
            }

            template< template< typename... > class Action, typename Input, typename... States >
            static auto apply0( const Input& in, trace_state& ts, States&&... st )
               -> decltype( apply0< Action >( in, st... ) )
            {
               fprintf( stderr,  "%6d        ", ++ts.line );
               return apply0< Action >( in, st... );
            }
         };
      };

      template< typename Rule >
      using tracer = trace< normal >::control< Rule >;

#endif

   }  // namespace TAO_PEGTL_NAMESPACE

}  // namespace tao

#endif
