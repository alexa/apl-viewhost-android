# The Art of C++
# Copyright (c) 2014-2020 Dr. Colin Hirsch and Daniel Frey
# Please see LICENSE for license or visit https://github.com/taocpp/PEGTL

.SUFFIXES:
.SECONDARY:

ifeq ($(OS),Windows_NT)
UNAME_S := $(OS)
ifeq ($(shell gcc -dumpmachine),mingw32)
MINGW_CXXFLAGS = -U__STRICT_ANSI__
endif
else
UNAME_S := $(shell uname -s)
endif

# For Darwin (Mac OS X / macOS) we assume that the default compiler
# clang++ is used; when $(CXX) is some version of g++, then
# $(CXXSTD) has to be set to -std=c++11 (or newer) so
# that -stdlib=libc++ is not automatically added.

ifeq ($(CXXSTD),)
CXXSTD := -std=c++11
ifeq ($(UNAME_S),Darwin)
CXXSTD += -stdlib=libc++
endif
endif

# Ensure strict standard compliance and no warnings, can be
# changed if desired.

CPPFLAGS ?= -pedantic
CXXFLAGS ?= -Wall -Wextra -Wshadow -Werror -O3 $(MINGW_CXXFLAGS)

CLANG_TIDY ?= clang-tidy

HEADERS := $(shell find include -name '*.hpp')
SOURCES := $(shell find src -name '*.cpp')
DEPENDS := $(SOURCES:%.cpp=build/%.d)
BINARIES := $(SOURCES:%.cpp=build/%)

CLANG_TIDY_HEADERS := $(filter-out include/tao/pegtl/internal/endian_win.hpp include/tao/pegtl/internal/file_mapper_win32.hpp,$(HEADERS))
CLANG_TIDY_SOURCES := $(filter-out src/example/pegtl/abnf2pegtl.cpp src/example/pegtl/lua53_parse.cpp,$(SOURCES))

UNIT_TESTS := $(filter build/src/test/%,$(BINARIES))

.PHONY: all
all: compile check

.PHONY: compile
compile: $(BINARIES)

.PHONY: check
check: $(UNIT_TESTS)
	@set -e; for T in $(UNIT_TESTS); do echo $$T; $$T > /dev/null; done

build/%.valgrind: build/%
	valgrind --error-exitcode=1 --leak-check=full $<
	@touch $@

.PHONY: valgrind
valgrind: $(UNIT_TESTS:%=%.valgrind)
	@echo "All $(words $(UNIT_TESTS)) valgrind tests passed."

build/%.clang-tidy: % .clang-tidy
	$(CLANG_TIDY) -quiet $< -- $(CXXSTD) -Iinclude $(CPPFLAGS) $(CXXFLAGS) 2>/dev/null
	@mkdir -p $(@D)
	@touch $@

.PHONY: clang-tidy
clang-tidy: $(CLANG_TIDY_HEADERS:%=build/%.clang-tidy) $(CLANG_TIDY_SOURCES:%=build/%.clang-tidy)
	@echo "All $(words $(CLANG_TIDY_HEADERS) $(CLANG_TIDY_SOURCES)) clang-tidy tests passed."

.PHONY: clean
clean:
	@rm -rf build
	@find . -name '*~' -delete

build/%.d: %.cpp Makefile
	@mkdir -p $(@D)
	$(CXX) $(CXXSTD) -Iinclude $(CPPFLAGS) -MM -MQ $@ $< -o $@

build/%: %.cpp build/%.d
	$(CXX) $(CXXSTD) -Iinclude $(CPPFLAGS) $(CXXFLAGS) $< -o $@

.PHONY: amalgamate
amalgamate: build/amalgamated/pegtl.hpp

build/amalgamated/pegtl.hpp: $(HEADERS)
	@mkdir -p $(@D)
	@rm -rf build/include
	@cp -a include build/
	@rm -rf build/include/tao/pegtl/contrib/icu
	@rm -rf build/include/tao/pegtl/contrib/changes.hpp
	@sed -i -e 's%^#%//#%g' $$(find build/include -name '*.hpp')
	@sed -i -e 's%^//#include "%#include "%g' $$(find build/include -name '*.hpp')
	@for i in $$(find build/include -name '*.hpp'); do echo "#pragma once" >tmp.out; echo "#line 1" >>tmp.out; cat $$i >>tmp.out; mv tmp.out $$i; done
	@echo '#include "tao/pegtl.hpp"' >build/include/amalgamated.hpp
	@echo '#include "tao/pegtl/analyze.hpp"' >>build/include/amalgamated.hpp
	@( cd build/include ; for i in tao/pegtl/contrib/*.hpp; do echo "#include \"$$i\""; done ) >>build/include/amalgamated.hpp
	@echo -e "/*\n\nWelcome to the Parsing Expression Grammar Template Library (PEGTL)." >$@
	@echo -e "See https://github.com/taocpp/PEGTL/ for more information, documentation, etc.\n" >>$@
	@echo -e "The library is licensed as follows:\n" >>$@
	@cat LICENSE >>$@
	@echo -e "\n*/\n" >>$@
	@( cd build/include ; g++ -E -C -nostdinc amalgamated.hpp ) >>$@
	@sed -i -e 's%^//#%#%g' $@
	@sed -i -e 's%^# \([0-9]* "[^"]*"\).*%#line \1%g' $@
	@sed -i -e 's%^// Copyright.*%%g' $@
	@sed -i -e 's%^// Please.*%%g' $@
	@echo "Generated/updated $@ successfully."

ifeq ($(findstring $(MAKECMDGOALS),clean),)
-include $(DEPENDS)
endif
