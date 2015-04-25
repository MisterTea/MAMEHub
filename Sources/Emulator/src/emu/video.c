// license:BSD-3-Clause
// copyright-holders:Aaron Giles
/***************************************************************************

    video.c

    Core MAME video routines.

***************************************************************************/

#include "WebMEncoder.h"

#include "NSM_Server.h"
#include "NSM_Client.h"
#include "emu.h"
#include "emuopts.h"
#include "png.h"
#include "debugger.h"
#include "ui/ui.h"
#include "aviio.h"
#include "crsshair.h"
#include "rendersw.inc"
#include "output.h"

#include "snap.lh"

#include "osdepend.h"

//**************************************************************************
//  DEBUGGING
//**************************************************************************

#define LOG_THROTTLE                (0)



//**************************************************************************
//  GLOBAL VARIABLES
//**************************************************************************

// frameskipping tables
const UINT8 video_manager::s_skiptable[FRAMESKIP_LEVELS][FRAMESKIP_LEVELS] =
{
	{ 0,0,0,0,0,0,0,0,0,0,0,0 },
	{ 0,0,0,0,0,0,0,0,0,0,0,1 },
	{ 0,0,0,0,0,1,0,0,0,0,0,1 },
	{ 0,0,0,1,0,0,0,1,0,0,0,1 },
	{ 0,0,1,0,0,1,0,0,1,0,0,1 },
	{ 0,1,0,0,1,0,1,0,0,1,0,1 },
	{ 0,1,0,1,0,1,0,1,0,1,0,1 },
	{ 0,1,0,1,1,0,1,0,1,1,0,1 },
	{ 0,1,1,0,1,1,0,1,1,0,1,1 },
	{ 0,1,1,1,0,1,1,1,0,1,1,1 },
	{ 0,1,1,1,1,1,0,1,1,1,1,1 },
	{ 0,1,1,1,1,1,1,1,1,1,1,1 }
};



//**************************************************************************
//  VIDEO MANAGER
//**************************************************************************

static void video_notifier_callback(const char *outname, INT32 value, void *param)
{
	video_manager *vm = (video_manager *)param;

	vm->set_output_changed();
}


//-------------------------------------------------
//  video_manager - constructor
//-------------------------------------------------

video_manager::video_manager(running_machine &machine)
	: m_machine(machine),
		m_screenless_frame_timer(NULL),
		m_output_changed(false),
		m_throttle_last_ticks(0),
		m_throttle_realtime(attotime::zero),
		m_throttle_emutime(attotime::zero),
		m_throttle_history(0),
		m_speed_last_realtime(0),
		m_speed_last_emutime(attotime::zero),
		m_speed_percent(1.0),
		m_overall_real_seconds(0),
		m_overall_real_ticks(0),
		m_overall_emutime(attotime::zero),
		m_overall_valid_counter(0),
		m_throttled(machine.options().throttle()),
		m_throttle_rate(1.0f),
		m_fastforward(false),
		m_seconds_to_run(machine.options().seconds_to_run()),
		m_auto_frameskip(machine.options().auto_frameskip()),
		m_speed(original_speed_setting()),
		m_empty_skip_count(0),
		m_frameskip_level(machine.options().frameskip()),
		m_frameskip_counter(0),
		m_frameskip_adjust(0),
		m_skipping_this_frame(false),
		m_average_oversleep(0),
		m_snap_target(NULL),
	  m_snap_bitmap(NULL),
		m_snap_native(true),
		m_snap_width(0),
		m_snap_height(0),
		m_mng_frame_period(attotime::zero),
		m_mng_next_frame_time(attotime::zero),
		m_mng_frame(0),
		m_avi_file(NULL),
      m_webmencoder(NULL),
		m_avi_frame_period(attotime::zero),
		m_avi_next_frame_time(attotime::zero),
		m_avi_frame(0),
		m_dummy_recording(false)
{
	// request a callback upon exiting
	machine.add_notifier(MACHINE_NOTIFY_EXIT, machine_notify_delegate(FUNC(video_manager::exit), this));
	machine.save().register_postload(save_prepost_delegate(FUNC(video_manager::postload), this));

	// extract initial execution state from global configuration settings
	update_refresh_speed();

	// create a render target for snapshots
	const char *viewname = machine.options().snap_view();
	m_snap_native = (machine.first_screen() != NULL && (viewname[0] == 0 || strcmp(viewname, "native") == 0));

	// the native target is hard-coded to our internal layout and has all options disabled
	if (m_snap_native)
	{
		m_snap_target = machine.render().target_alloc(layout_snap, RENDER_CREATE_SINGLE_FILE | RENDER_CREATE_HIDDEN);
		m_snap_target->set_backdrops_enabled(false);
		m_snap_target->set_overlays_enabled(false);
		m_snap_target->set_bezels_enabled(false);
		m_snap_target->set_cpanels_enabled(false);
		m_snap_target->set_marquees_enabled(false);
		m_snap_target->set_screen_overlay_enabled(false);
		m_snap_target->set_zoom_to_screen(false);
	}

	// other targets select the specified view and turn off effects
	else
	{
		m_snap_target = machine.render().target_alloc(NULL, RENDER_CREATE_HIDDEN);
		m_snap_target->set_view(m_snap_target->configured_view(viewname, 0, 1));
		m_snap_target->set_screen_overlay_enabled(false);
	}

	// extract snap resolution if present
	if (sscanf(machine.options().snap_size(), "%dx%d", &m_snap_width, &m_snap_height) != 2)
		m_snap_width = m_snap_height = 0;

	// start recording movie if specified
	const char *filename = machine.options().mng_write();
	if (filename[0] != 0)
		begin_recording(filename, MF_MNG);

	filename = machine.options().avi_write();
	if (filename[0] != 0)
		begin_recording(filename, MF_AVI);

#ifdef MAME_DEBUG
	m_dummy_recording = machine.options().dummy_write();
#endif

	// if no screens, create a periodic timer to drive updates
	if (machine.first_screen() == NULL)
	{
		m_screenless_frame_timer = machine.scheduler().timer_alloc(timer_expired_delegate(FUNC(video_manager::screenless_update_callback), this));
		m_screenless_frame_timer->adjust(screen_device::DEFAULT_FRAME_PERIOD, 0, screen_device::DEFAULT_FRAME_PERIOD);
		output_set_notifier(NULL, video_notifier_callback, this);
	}
}


//-------------------------------------------------
//  set_frameskip - set the current actual
//  frameskip (-1 means autoframeskip)
//-------------------------------------------------

void video_manager::set_frameskip(int frameskip)
{
	//Can't skip frames in client-server mode
	if(netCommon)
	{
		m_auto_frameskip = false;
		m_frameskip_level = 0;
	}
	// -1 means autoframeskip
	else if (frameskip == -1)
	{
		m_auto_frameskip = true;
		m_frameskip_level = 0;
	}

	// any other level is a direct control
	else if (frameskip >= 0 && frameskip <= MAX_FRAMESKIP)
	{
		m_auto_frameskip = false;
		m_frameskip_level = frameskip;
	}
}

extern bool waitingForClientCatchup;

//-------------------------------------------------
//  frame_update - handle frameskipping and UI,
//  plus updating the screen during normal
//  operations
//-------------------------------------------------

bool SKIP_OSD=false;

void video_manager::frame_update(bool debug)
{
	// only render sound and video if we're in the running phase
	int phase = machine().phase();
	bool skipped_it = m_skipping_this_frame;
	if (phase == MACHINE_PHASE_RUNNING && (!machine().paused() || machine().options().update_in_pause()))
	{
		bool anything_changed = finish_screen_updates();

		// if none of the screens changed and we haven't skipped too many frames in a row,
		// mark this frame as skipped to prevent throttling; this helps for games that
		// don't update their screen at the monitor refresh rate
		if (!anything_changed && !m_auto_frameskip && m_frameskip_level == 0 && m_empty_skip_count++ < 3)
			skipped_it = true;
		else
			m_empty_skip_count = 0;
	}

	if(netCommon)
	{
		//Can't skip frames in network mode
		skipped_it = false;
	}
	// draw the user interface
	machine().ui().update_and_render(&machine().render().ui_container());

	// if we're throttling, synchronize before rendering

	attotime current_time = machine().machine_time();
	//Don't throttle if you are a network client
	if (!debug && !skipped_it && effective_throttle())
		update_throttle(current_time);

	// ask the OSD to update
	g_profiler.start(PROFILER_BLIT);
	machine().osd().update(!debug && (skipped_it));
	g_profiler.stop();

	machine().manager().lua()->periodic_check();

	// perform tasks for this frame
	if (!debug)
		machine().call_notifiers(MACHINE_NOTIFY_FRAME);

	// update frameskipping
	if (!netCommon && !debug)
		update_frameskip();

	// update speed computations
	if (!debug)
		recompute_speed(current_time);

	// call the end-of-frame callback
	if (phase == MACHINE_PHASE_RUNNING)
	{
		// reset partial updates if we're paused or if the debugger is active
		if (machine().first_screen() != NULL && (machine().paused() || debug || debugger_within_instruction_hook(machine())))
			machine().first_screen()->reset_partial_updates();
	}
}


//-------------------------------------------------
//  speed_text - print the text to be displayed
//  into a string buffer
//-------------------------------------------------

astring &video_manager::speed_text(astring &string)
{
	string.reset();

	// if we're paused, just display Paused
	bool paused = machine().paused();
	if (paused)
		string.cat("paused");

	// if we're fast forwarding, just display Fast-forward
	else if (m_fastforward)
		string.cat("fast ");

	// if we're auto frameskipping, display that plus the level
	else if (effective_autoframeskip())
		string.catprintf("auto%2d/%d", effective_frameskip(), MAX_FRAMESKIP);

	// otherwise, just display the frameskip plus the level
	else
		string.catprintf("skip %d/%d", effective_frameskip(), MAX_FRAMESKIP);

	// append the speed for all cases except paused
	if (!paused)
		string.catprintf("%4d%%", (int)(100 * m_speed_percent + 0.5));

	// display the number of partial updates as well
	int partials = 0;
	screen_device_iterator iter(machine().root_device());
	for (screen_device *screen = iter.first(); screen != NULL; screen = iter.next())
		partials += screen->partial_updates();
	if (partials > 1)
		string.catprintf("\n%d partial updates", partials);

	return string;
}


//-------------------------------------------------
//  save_snapshot - save a snapshot to the given
//  file handle
//-------------------------------------------------

void video_manager::save_snapshot(screen_device *screen, emu_file &file)
{
	// validate
	assert(!m_snap_native || screen != NULL);

	// create the bitmap to pass in
	create_snapshot_bitmap(screen);

	// add two text entries describing the image
	astring text1(emulator_info::get_appname(), " ", build_version);
	astring text2(machine().system().manufacturer, " ", machine().system().description);
	png_info pnginfo = { 0 };
	png_add_text(&pnginfo, "Software", text1);
	png_add_text(&pnginfo, "System", text2);

	// now do the actual work
	const rgb_t *palette = (screen !=NULL && screen->palette() != NULL) ? screen->palette()->palette()->entry_list_adjusted() : NULL;
	int entries = (screen !=NULL && screen->palette() != NULL) ? screen->palette()->entries() : 0;
	png_error error = png_write_bitmap(file, &pnginfo, m_snap_bitmap, entries, palette);
	if (error != PNGERR_NONE)
		osd_printf_error("Error generating PNG for snapshot: png_error = %d\n", error);

	// free any data allocated
	png_free(&pnginfo);
}


int createSnapshot = 0;
//-------------------------------------------------
//  save_active_screen_snapshots - save a
//  snapshot of all active screens
//-------------------------------------------------

void video_manager::save_active_screen_snapshots()
{
  createSnapshot = 1;
}

void video_manager::save_active_screen_snapshots_real()
{
	// if we're native, then write one snapshot per visible screen
	if (m_snap_native)
	{
		// write one snapshot per visible screen
		screen_device_iterator iter(machine().root_device());
		for (screen_device *screen = iter.first(); screen != NULL; screen = iter.next())
			if (machine().render().is_live(*screen))
			{
				emu_file file(machine().options().snapshot_directory(), OPEN_FLAG_WRITE | OPEN_FLAG_CREATE | OPEN_FLAG_CREATE_PATHS);
				file_error filerr = open_next(file, "png");
				if (filerr == FILERR_NONE)
					save_snapshot(screen, file);
			}
	}

	// otherwise, just write a single snapshot
	else
	{
		emu_file file(machine().options().snapshot_directory(), OPEN_FLAG_WRITE | OPEN_FLAG_CREATE | OPEN_FLAG_CREATE_PATHS);
		file_error filerr = open_next(file, "png");
		if (filerr == FILERR_NONE)
			save_snapshot(NULL, file);
	}
}


//-------------------------------------------------
//  begin_recording - begin recording of a movie
//-------------------------------------------------

void video_manager::begin_recording(const char *name, movie_format format)
{
	// create a snapshot bitmap so we know what the target size is
	create_snapshot_bitmap(NULL);

	// start up an AVI recording
	if (format == MF_AVI)
	{
		// stop any existing recording
		end_recording(format);

		// reset the state
		m_avi_frame = 0;
		m_avi_next_frame_time = machine().machine_time();

		// build up information about this new movie
		avi_movie_info info;
		info.video_format = 0;
		info.video_timescale = 1000 * ((machine().first_screen() != NULL) ? ATTOSECONDS_TO_HZ(machine().first_screen()->frame_period().attoseconds) : screen_device::DEFAULT_FRAME_RATE);
		info.video_sampletime = 1000;
		info.video_numsamples = 0;
		info.video_width = m_snap_bitmap.width();
		info.video_height = m_snap_bitmap.height();
		info.video_depth = 24;

		info.audio_format = 0;
		info.audio_timescale = machine().sample_rate();
		info.audio_sampletime = 1;
		info.audio_numsamples = 0;
		info.audio_channels = 2;
		info.audio_samplebits = 16;
		info.audio_samplerate = machine().sample_rate();

		// create a new temporary movie file
		file_error filerr;
		astring fullpath;
		{
			emu_file tempfile(machine().options().snapshot_directory(), OPEN_FLAG_WRITE | OPEN_FLAG_CREATE | OPEN_FLAG_CREATE_PATHS);
			if (name != NULL)
				filerr = tempfile.open(name);
			else
				filerr = open_next(tempfile, "avi");

			// if we succeeded, make a copy of the name and create the real file over top
			if (filerr == FILERR_NONE)
				fullpath = tempfile.fullpath();
		}

		if (filerr == FILERR_NONE)
		{
			// compute the frame time
			m_avi_frame_period = attotime::from_seconds(1000) / info.video_timescale;

			// create the file and free the string
			avi_error avierr = avi_create(fullpath, &info, &m_avi_file);
			if (avierr != AVIERR_NONE)
			{
				osd_printf_error("Error creating AVI: %s\n", avi_error_string(avierr));
				return end_recording(format);
			}
		}
	}

	// start up a MNG recording
	else if (format == MF_MNG)
	{
		// stop any existing recording
		end_recording(format);

		// reset the state
		m_mng_frame = 0;
		m_mng_next_frame_time = machine().machine_time();

		// create a new movie file and start recording
		m_mng_file.reset(global_alloc(emu_file(machine().options().snapshot_directory(), OPEN_FLAG_WRITE | OPEN_FLAG_CREATE | OPEN_FLAG_CREATE_PATHS)));
		file_error filerr;
		if (name != NULL)
			filerr = m_mng_file->open(name);
		else
			filerr = open_next(*m_mng_file, "mng");

		if (filerr == FILERR_NONE)
		{
			// start the capture
			int rate = (machine().first_screen() != NULL) ? ATTOSECONDS_TO_HZ(machine().first_screen()->frame_period().attoseconds) : screen_device::DEFAULT_FRAME_RATE;
			png_error pngerr = mng_capture_start(*m_mng_file, m_snap_bitmap, rate);
			if (pngerr != PNGERR_NONE)
			{
				osd_printf_error("Error capturing MNG, png_error=%d\n", pngerr);
				return end_recording(format);
			}

			// compute the frame time
			m_mng_frame_period = attotime::from_hz(rate);
		}
		else
		{
			osd_printf_error("Error creating MNG, file_error=%d\n", filerr);
			m_mng_file.reset();
		}
	}
	// start up an WEBM recording
	else if (format == MF_WEBM)
	{
		// stop any existing recording
		end_recording(format);

		// reset the state
		m_webm_frame = 0;
		m_webm_next_frame_time = machine().machine_time();

		// create a new temporary movie file
		file_error filerr;
		astring fullpath;
		{
			emu_file tempfile(machine().options().snapshot_directory(), OPEN_FLAG_WRITE | OPEN_FLAG_CREATE | OPEN_FLAG_CREATE_PATHS);
			if (name != NULL)
				filerr = tempfile.open(name);
			else
				filerr = open_next(tempfile, "webm");

			// compute the frame time
			m_webm_frame_period.seconds = 0;
			m_webm_frame_period.attoseconds = ATTOSECONDS_PER_SECOND / 30;

			// if we succeeded, make a copy of the name and create the real file over top
			if (filerr == FILERR_NONE)
				fullpath = tempfile.fullpath();
		}

		if (filerr == FILERR_NONE)
		{
            m_webmencoder = global_alloc(WebMEncoder(fullpath.cstr(),m_snap_bitmap.width(),m_snap_bitmap.height(),WebMEncoder::IVF_RGBA,1,30,machine().sample_rate(),2));
		}
	}

}


//-------------------------------------------------
//  end_recording - stop recording of a movie
//-------------------------------------------------

void video_manager::end_recording(movie_format format)
{
	if (format == MF_AVI)
	{
		// close the file if it exists
		if (m_avi_file != NULL)
		{
			avi_close(m_avi_file);
			m_avi_file = NULL;

			// reset the state
			m_avi_frame = 0;
		}
	}
	else if (format == MF_MNG)
	{
		// close the file if it exists
		if (m_mng_file != NULL)
		{
			mng_capture_stop(*m_mng_file);
			m_mng_file.reset();

			// reset the state
			m_mng_frame = 0;
		}
	}
	else if (format == MF_WEBM) {
        global_free(m_webmencoder);
        m_webmencoder = NULL;

		m_webm_frame = 0;
    }
}


//-------------------------------------------------
//  add_sound_to_recording - add sound to a movie
//  recording
//-------------------------------------------------

void video_manager::add_sound_to_recording(const INT16 *sound, int numsamples)
{
	// only record if we have a file
	if (m_avi_file != NULL)
	{
		g_profiler.start(PROFILER_MOVIE_REC);

		// write the next frame
		avi_error avierr = avi_append_sound_samples(m_avi_file, 0, sound + 0, numsamples, 1);
		if (avierr == AVIERR_NONE)
			avierr = avi_append_sound_samples(m_avi_file, 1, sound + 1, numsamples, 1);
		if (avierr != AVIERR_NONE)
			end_recording(MF_AVI);

		g_profiler.stop();
	}
    if(m_webmencoder != NULL) {
        m_webmencoder->encodeAudioInterleaved(sound,numsamples);
    }
}



//-------------------------------------------------
//  video_exit - close down the video system
//-------------------------------------------------

void video_manager::exit()
{
	// stop recording any movie
	end_recording(MF_AVI);
	end_recording(MF_MNG);
	end_recording(MF_WEBM);

	// free the snapshot target
	machine().render().target_free(m_snap_target);
	m_snap_bitmap.reset();

	// print a final result if we have at least 2 seconds' worth of data
	if (m_overall_emutime.seconds >= 1)
	{
		osd_ticks_t tps = osd_ticks_per_second();
		double final_real_time = (double)m_overall_real_seconds + (double)m_overall_real_ticks / (double)tps;
		double final_emu_time = m_overall_emutime.as_double();
		osd_printf_info("Average speed: %.2f%% (%d seconds)\n", 100 * final_emu_time / final_real_time, (m_overall_emutime + attotime(0, ATTOSECONDS_PER_SECOND / 2)).seconds);
	}
}


//-------------------------------------------------
//  screenless_update_callback - update generator
//  when there are no screens to drive it
//-------------------------------------------------

void video_manager::screenless_update_callback(void *ptr, int param)
{
	// force an update
	frame_update(false);
}


//-------------------------------------------------
//  postload - callback for resetting things after
//  state has been loaded
//-------------------------------------------------

void video_manager::postload()
{
	m_avi_next_frame_time = machine().machine_time();
	m_mng_next_frame_time = machine().machine_time();
	m_webm_next_frame_time = machine().machine_time();
}


//-------------------------------------------------
//  effective_autoframeskip - return the effective
//  autoframeskip value, accounting for fast
//  forward
//-------------------------------------------------

inline int video_manager::effective_autoframeskip() const
{
	// if we're fast forwarding or paused, autoframeskip is disabled
	if (m_fastforward || machine().paused())
		return false;

	// otherwise, it's up to the user
	return m_auto_frameskip;
}


//-------------------------------------------------
//  effective_frameskip - return the effective
//  frameskip value, accounting for fast
//  forward
//-------------------------------------------------

inline int video_manager::effective_frameskip() const
{
	// if we're fast forwarding, use the maximum frameskip
	if (m_fastforward)
		return FRAMESKIP_LEVELS - 1;

	// otherwise, it's up to the user
	return m_frameskip_level;
}


//-------------------------------------------------
//  effective_throttle - return the effective
//  throttle value, accounting for fast
//  forward and user interface
//-------------------------------------------------

inline bool video_manager::effective_throttle() const
{
	// if we're paused, or if the UI is active, we always throttle
	if (machine().paused() || machine().ui().is_menu_active())
		return true;

	// if we're fast forwarding, we don't throttle
	if (m_fastforward)
		return false;

	// otherwise, it's up to the user
	return throttled();
}


//-------------------------------------------------
//  original_speed_setting - return the original
//  speed setting
//-------------------------------------------------

inline int video_manager::original_speed_setting() const
{
	return machine().options().speed() * 1000.0 + 0.5;
}


//-------------------------------------------------
//  finish_screen_updates - finish updating all
//  the screens
//-------------------------------------------------

bool video_manager::finish_screen_updates()
{
	// finish updating the screens
	screen_device_iterator iter(machine().root_device());

	for (screen_device *screen = iter.first(); screen != NULL; screen = iter.next())
		screen->update_partial(screen->visible_area().max_y);

	// now add the quads for all the screens
	bool anything_changed = m_output_changed;
	m_output_changed = false;
	for (screen_device *screen = iter.first(); screen != NULL; screen = iter.next())
		if (screen->update_quads())
			anything_changed = true;

	// draw HUD from LUA callback (if any)
	anything_changed |= machine().manager().lua()->frame_hook();

	// update our movie recording and burn-in state
	if (!machine().paused())
	{
	  if(createSnapshot) {
	    save_active_screen_snapshots_real();
	    createSnapshot = 0;
	  }
		record_frame();

		// iterate over screens and update the burnin for the ones that care
		for (screen_device *screen = iter.first(); screen != NULL; screen = iter.next())
			screen->update_burnin();
	}

	// draw any crosshairs
	for (screen_device *screen = iter.first(); screen != NULL; screen = iter.next())
		crosshair_render(*screen);

	return anything_changed;
}



//-------------------------------------------------
//  update_throttle - throttle to the game's
//  natural speed
//-------------------------------------------------

INT64 realtimeEmulationShift = 0;
extern RakNet::Time emulationStartTime;

void video_manager::update_throttle(attotime emutime)
{
  // For mamehub we need to do something different
  const attoseconds_t attoseconds_per_tick = ATTOSECONDS_PER_SECOND / osd_ticks_per_second();
	{
    bool printed=false;

    while(true) {
      // Get current ticks
      RakNet::Time curTime = RakNet::GetTimeMS() - emulationStartTime;
      if (netClient) {
        curTime = netClient->getCurrentServerTime();
		}
      //cout << "Current time is: " << curTime << endl;
      //osd_ticks_t currentTicks = osd_ticks() - realtimeEmulationShift;

      // Convert ticks to emulation time
      attotime expectedEmulationTime(
        curTime/1000, // milliseconds to seconds
        (curTime%1000)*ATTOSECONDS_PER_MILLISECOND); // milliseconds to attoseconds
        //currentTicks/osd_ticks_per_second(),
        //(currentTicks%osd_ticks_per_second())*attoseconds_per_tick);

      if (expectedEmulationTime < emutime) {
        if (SKIP_OSD) {
          SKIP_OSD=false;
          cout << "We are caught up " << ((emutime - expectedEmulationTime).attoseconds/ATTOSECONDS_PER_MILLISECOND) << "ms" << endl;
		}
        if (!printed) {
          printed=true;
        }
        attotime tolerance(0,16*ATTOSECONDS_PER_MILLISECOND);
        if ((emutime - expectedEmulationTime) < tolerance) {
          //cout << "Returning " << ((emutime - expectedEmulationTime).attoseconds/ATTOSECONDS_PER_MILLISECOND) << endl;
          return;
		}

        if (netClient) {
          // Sleep for 15 ms and return
          osd_sleep((osd_ticks_per_second()/1000)*15);
          return;
        } else {
          // Sleep the processor 1ms and check again
          osd_sleep((osd_ticks_per_second()/1000));
          continue;
		}

      } else {
        attotime diffTime = expectedEmulationTime - emutime;

        int msBehind = (diffTime.attoseconds/ATTOSECONDS_PER_MILLISECOND) + diffTime.seconds*1000;

        if (msBehind > 100 && emutime.seconds>0) {
          static int lastSecondBehind = 0;
          if (lastSecondBehind < emutime.seconds) {
            cout << "We are behind " << msBehind << "ms.  Skipping video." << endl;
            lastSecondBehind = emutime.seconds;
		}
          SKIP_OSD=true;
        }
			return;
      }
    }
  }
	}

void video_manager::rollback(attotime rollbackAmount) {
  osd_ticks_t ticks_per_second = osd_ticks_per_second();
  attoseconds_t attoseconds_per_tick = ATTOSECONDS_PER_SECOND / ticks_per_second * m_throttle_rate;
  realtimeEmulationShift -= rollbackAmount.seconds*ticks_per_second;
  realtimeEmulationShift -= rollbackAmount.attoseconds/attoseconds_per_tick;
}

//-------------------------------------------------
//  throttle_until_ticks - spin until the
//  specified target time, calling the OSD code
//  to sleep if possible
//-------------------------------------------------

osd_ticks_t video_manager::throttle_until_ticks(osd_ticks_t target_ticks)
{
	// we're allowed to sleep via the OSD code only if we're configured to do so
	// and we're not frameskipping due to autoframeskip, or if we're paused
	bool allowed_to_sleep = false;
	if (machine().options().sleep() && (!effective_autoframeskip() || effective_frameskip() == 0))
		allowed_to_sleep = true;
	if (machine().paused())
		allowed_to_sleep = true;

	// loop until we reach our target
	g_profiler.start(PROFILER_IDLE);
	osd_ticks_t minimum_sleep = osd_ticks_per_second() / 1000;
	osd_ticks_t current_ticks = osd_ticks();
	while (current_ticks < target_ticks)
	{
		// compute how much time to sleep for, taking into account the average oversleep
		osd_ticks_t delta = (target_ticks - current_ticks) * 1000 / (1000 + m_average_oversleep);

		// see if we can sleep
		bool slept = false;
		if (allowed_to_sleep && delta >= minimum_sleep)
		{
			osd_sleep(delta);
			slept = true;
		}

		// read the new value
		osd_ticks_t new_ticks = osd_ticks();

		// keep some metrics on the sleeping patterns of the OSD layer
		if (slept)
		{
			// if we overslept, keep an average of the amount
			osd_ticks_t actual_ticks = new_ticks - current_ticks;
			if (actual_ticks > delta)
			{
				// take 90% of the previous average plus 10% of the new value
				osd_ticks_t oversleep_milliticks = 1000 * (actual_ticks - delta) / delta;
				m_average_oversleep = (m_average_oversleep * 99 + oversleep_milliticks) / 100;

				if (LOG_THROTTLE)
					logerror("Slept for %d ticks, got %d ticks, avgover = %d\n", (int)delta, (int)actual_ticks, (int)m_average_oversleep);
			}
		}
		current_ticks = new_ticks;
	}
	g_profiler.stop();

	return current_ticks;
}


//-------------------------------------------------
//  update_frameskip - update frameskipping
//  counters and periodically update autoframeskip
//-------------------------------------------------

void video_manager::update_frameskip()
{
	// if we're throttling and autoframeskip is on, adjust
	if (effective_throttle() && effective_autoframeskip() && m_frameskip_counter == 0)
	{
		// calibrate the "adjusted speed" based on the target
		double adjusted_speed_percent = m_speed_percent / m_throttle_rate;

		// if we're too fast, attempt to increase the frameskip
		double speed = m_speed * 0.001;
		if (adjusted_speed_percent >= 0.995 * speed)
		{
			// but only after 3 consecutive frames where we are too fast
			if (++m_frameskip_adjust >= 3)
			{
				m_frameskip_adjust = 0;
				if (m_frameskip_level > 0)
					m_frameskip_level--;
			}
		}

		// if we're too slow, attempt to increase the frameskip
		else
		{
			// if below 80% speed, be more aggressive
			if (adjusted_speed_percent < 0.80 *  speed)
				m_frameskip_adjust -= (0.90 * speed - m_speed_percent) / 0.05;

			// if we're close, only force it up to frameskip 8
			else if (m_frameskip_level < 8)
				m_frameskip_adjust--;

			// perform the adjustment
			while (m_frameskip_adjust <= -2)
			{
				m_frameskip_adjust += 2;
				if (m_frameskip_level < MAX_FRAMESKIP)
					m_frameskip_level++;
			}
		}
	}

	// increment the frameskip counter and determine if we will skip the next frame
	m_frameskip_counter = (m_frameskip_counter + 1) % FRAMESKIP_LEVELS;
	m_skipping_this_frame = s_skiptable[effective_frameskip()][m_frameskip_counter];
}


//-------------------------------------------------
//  update_refresh_speed - update the m_speed
//  based on the maximum refresh rate supported
//-------------------------------------------------

void video_manager::update_refresh_speed()
{
	// only do this if the refreshspeed option is used
	if (machine().options().refresh_speed() && !machine().options().client() && !machine().options().server())
	{
		float minrefresh = machine().render().max_update_rate();
		if (minrefresh != 0)
		{
			// find the screen with the shortest frame period (max refresh rate)
			// note that we first check the token since this can get called before all screens are created
			attoseconds_t min_frame_period = ATTOSECONDS_PER_SECOND;
			screen_device_iterator iter(machine().root_device());
			for (screen_device *screen = iter.first(); screen != NULL; screen = iter.next())
			{
				attoseconds_t period = screen->frame_period().attoseconds;
				if (period != 0)
					min_frame_period = MIN(min_frame_period, period);
			}

			// compute a target speed as an integral percentage
			// note that we lop 0.25Hz off of the minrefresh when doing the computation to allow for
			// the fact that most refresh rates are not accurate to 10 digits...
			UINT32 target_speed = floor((minrefresh - 0.25f) * 1000.0 / ATTOSECONDS_TO_HZ(min_frame_period));
			UINT32 original_speed = original_speed_setting();
			target_speed = MIN(target_speed, original_speed);

			// if we changed, log that verbosely
			if (target_speed != m_speed)
			{
				osd_printf_verbose("Adjusting target speed to %.1f%% (hw=%.2fHz, game=%.2fHz, adjusted=%.2fHz)\n", target_speed / 10.0, minrefresh, ATTOSECONDS_TO_HZ(min_frame_period), ATTOSECONDS_TO_HZ(min_frame_period * 1000.0 / target_speed));
				m_speed = target_speed;
			}
		}
	}
}


//-------------------------------------------------
//  recompute_speed - recompute the current
//  overall speed; we assume this is called only
//  if we did not skip a frame
//-------------------------------------------------

void video_manager::recompute_speed(const attotime &emutime)
{
	// if we don't have a starting time yet, or if we're paused, reset our starting point
	if (m_speed_last_realtime == 0 || machine().paused())
	{
		m_speed_last_realtime = osd_ticks();
		m_speed_last_emutime = emutime;
	}

	// if it has been more than the update interval, update the time
	attotime delta_emutime = emutime - m_speed_last_emutime;
	if (delta_emutime > attotime(0, ATTOSECONDS_PER_SPEED_UPDATE))
	{
		// convert from ticks to attoseconds
		osd_ticks_t realtime = osd_ticks();
		osd_ticks_t delta_realtime = realtime - m_speed_last_realtime;
		osd_ticks_t tps = osd_ticks_per_second();
		m_speed_percent = delta_emutime.as_double() * (double)tps / (double)delta_realtime;

		// remember the last times
		m_speed_last_realtime = realtime;
		m_speed_last_emutime = emutime;

		// if we're throttled, this time period counts for overall speed; otherwise, we reset the counter
		if (!m_fastforward)
			m_overall_valid_counter++;
		else
			m_overall_valid_counter = 0;

		// if we've had at least 4 consecutive valid periods, accumulate stats
		if (m_overall_valid_counter >= 4)
		{
			m_overall_real_ticks += delta_realtime;
			while (m_overall_real_ticks >= tps)
			{
				m_overall_real_ticks -= tps;
				m_overall_real_seconds++;
			}
			m_overall_emutime += delta_emutime;
		}
	}

	// if we're past the "time-to-execute" requested, signal an exit
	if (m_seconds_to_run != 0 && emutime.seconds >= m_seconds_to_run)
	{
#ifdef MAME_DEBUG
		if (g_tagmap_counter_enabled)
		{
			g_tagmap_counter_enabled = false;
			if (*(machine().options().command()) == 0)
				osd_printf_info("%d tagmap lookups\n", g_tagmap_finds);
		}
#endif

		if (machine().first_screen() != NULL)
		{
			// create a final screenshot
			emu_file file(machine().options().snapshot_directory(), OPEN_FLAG_WRITE | OPEN_FLAG_CREATE | OPEN_FLAG_CREATE_PATHS);
			file_error filerr = file.open(machine().basename(), PATH_SEPARATOR "final.png");
			if (filerr == FILERR_NONE)
				save_snapshot(machine().first_screen(), file);
		}
		//printf("Scheduled exit at %f\n", emutime.as_double());
		// schedule our demise
		machine().schedule_exit();
	}
}


//-------------------------------------------------
//  create_snapshot_bitmap - creates a
//  bitmap containing the screenshot for the
//  given screen
//-------------------------------------------------

typedef software_renderer<UINT32, 0,0,0, 16,8,0, false, true> snap_renderer_bilinear;
typedef software_renderer<UINT32, 0,0,0, 16,8,0, false, false> snap_renderer;

void video_manager::create_snapshot_bitmap(screen_device *screen)
{
	// select the appropriate view in our dummy target
	if (m_snap_native && screen != NULL)
	{
		screen_device_iterator iter(machine().root_device());
		int view_index = iter.indexof(*screen);
		assert(view_index != -1);
		m_snap_target->set_view(view_index);
	}

	// get the minimum width/height and set it on the target
	INT32 width = m_snap_width;
	INT32 height = m_snap_height;
	if (width == 0 || height == 0)
		m_snap_target->compute_minimum_size(width, height);

	while(width<400 || height<300) {
	    width <<= 1;
	    height <<= 1;
	  }
	while(width>800 || height>600) {
	  width >>= 1;
	  height >>= 1;
	}
	// JJG: Make sure it's divisible by 16
  width = ((width>>4)<<4);
  height = ((height>>4)<<4);

	//cout << "WIDTH: " << width << " HEIGHT: " << height << endl;

	m_snap_target->set_bounds(width, height);

	// if we don't have a bitmap, or if it's not the right size, allocate a new one
	if (!m_snap_bitmap.valid() || width != m_snap_bitmap.width() || height != m_snap_bitmap.height())
		m_snap_bitmap.allocate(width, height);

	// render the screen there
	render_primitive_list &primlist = m_snap_target->get_primitives();
	primlist.acquire_lock();
	if (machine().options().snap_bilinear())
		snap_renderer_bilinear::draw_primitives(primlist, &m_snap_bitmap.pix32(0), width, height, m_snap_bitmap.rowpixels());
	else
		snap_renderer::draw_primitives(primlist, &m_snap_bitmap.pix32(0), width, height, m_snap_bitmap.rowpixels());
	primlist.release_lock();
}


//-------------------------------------------------
//  open_next - open the next non-existing file of
//  type filetype according to our numbering
//  scheme
//-------------------------------------------------

file_error video_manager::open_next(emu_file &file, const char *extension)
{
	UINT32 origflags = file.openflags();

	// handle defaults
	const char *snapname = machine().options().snap_name();

	if (snapname == NULL || snapname[0] == 0)
		snapname = "%g/%i";
	astring snapstr(snapname);

	// strip any extension in the provided name
	int index = snapstr.rchr(0, '.');
	if (index != -1)
		snapstr.substr(0, index);

	// handle %d in the template (for image devices)
	astring snapdev("%d_");
	int pos = snapstr.find(0, snapdev);

	if (pos != -1)
	{
		// if more %d are found, revert to default and ignore them all
		if (snapstr.find(pos + 3, snapdev) != -1)
			snapstr.cpy("%g/%i");
		// else if there is a single %d, try to create the correct snapname
		else
		{
			int name_found = 0;

			// find length of the device name
			int end1 = snapstr.find(pos + 3, "/");
			int end2 = snapstr.find(pos + 3, "%");
			int end = -1;

			if ((end1 != -1) && (end2 != -1))
				end = MIN(end1, end2);
			else if (end1 != -1)
				end = end1;
			else if (end2 != -1)
				end = end2;
			else
				end = snapstr.len();

			if (end - pos < 3)
				fatalerror("Something very wrong is going on!!!\n");

			// copy the device name to an astring
			astring snapdevname;
			snapdevname.cpysubstr(snapstr, pos + 3, end - pos - 3);
			//printf("check template: %s\n", snapdevname.cstr());

			// verify that there is such a device for this system
			image_interface_iterator iter(machine().root_device());
			for (device_image_interface *image = iter.first(); image != NULL; image = iter.next())
			{
				// get the device name
				astring tempdevname(image->brief_instance_name());
				//printf("check device: %s\n", tempdevname.cstr());

				if (snapdevname.cmp(tempdevname) == 0)
				{
					// verify that such a device has an image mounted
					if (image->basename() != NULL)
					{
						astring filename(image->basename());

						// strip extension
						filename.substr(0, filename.rchr(0, '.'));

						// setup snapname and remove the %d_
						snapstr.replace(0, snapdevname, filename);
						snapstr.del(pos, 3);
						//printf("check image: %s\n", filename.cstr());

						name_found = 1;
					}
				}
			}

			// or fallback to default
			if (name_found == 0)
				snapstr.cpy("%g/%i");
		}
	}

	// add our own extension
	snapstr.cat(".").cat(extension);

	// substitute path and gamename up front
	snapstr.replace(0, "/", PATH_SEPARATOR);
	snapstr.replace(0, "%g", machine().basename());

	// determine if the template has an index; if not, we always use the same name
	astring fname;
	if (snapstr.find(0, "%i") == -1)
		fname.cpy(snapstr);

	// otherwise, we scan for the next available filename
	else
	{
		// try until we succeed
		astring seqtext;
		file.set_openflags(OPEN_FLAG_READ);
		for (int seq = 0; ; seq++)
		{
			// build up the filename
			fname.cpy(snapstr).replace(0, "%i", seqtext.format("%04d", seq).cstr());

			// try to open the file; stop when we fail
			file_error filerr = file.open(fname);
			if (filerr != FILERR_NONE)
				break;
		}
	}

	// create the final file
	file.set_openflags(origflags);
	return file.open(fname);
}


//-------------------------------------------------
//  record_frame - record a frame of a movie
//-------------------------------------------------

void video_manager::record_frame()
{
	// ignore if nothing to do
	if (m_mng_file == NULL && m_avi_file == NULL && m_webmencoder == NULL && !m_dummy_recording)
		return;

	// start the profiler and get the current time
	g_profiler.start(PROFILER_MOVIE_REC);
	attotime curtime = machine().machine_time();

	// create the bitmap
	create_snapshot_bitmap(NULL);

	// handle an AVI recording
	if (m_avi_file != NULL)
	{
		// loop until we hit the right time
		while (m_avi_next_frame_time <= curtime)
		{
			// write the next frame
			avi_error avierr = avi_append_video_frame(m_avi_file, m_snap_bitmap);
			if (avierr != AVIERR_NONE)
			{
				g_profiler.stop();
				end_recording(MF_AVI);
				break;
			}

			// advance time
			m_avi_next_frame_time += m_avi_frame_period;
			m_avi_frame++;
		}
	}

	// handle an WEBM recording
	if (m_webmencoder != NULL)
	{
		// loop until we hit the right time
		while (m_webm_next_frame_time <= curtime)
		{
			// write the next frame
            m_webmencoder->encodeVideo((unsigned char*)(m_snap_bitmap.raw_pixptr(0,0)),m_snap_bitmap.rowpixels());

			// advance time
			m_webm_next_frame_time += m_webm_frame_period;
			m_webm_frame++;
		}
	}

	// handle a MNG recording
	if (m_mng_file != NULL)
	{
		// loop until we hit the right time
		while (m_mng_next_frame_time <= curtime)
		{
			// set up the text fields in the movie info
			png_info pnginfo = { 0 };
			if (m_mng_frame == 0)
			{
				astring text1(emulator_info::get_appname(), " ", build_version);
				astring text2(machine().system().manufacturer, " ", machine().system().description);
				png_add_text(&pnginfo, "Software", text1);
				png_add_text(&pnginfo, "System", text2);
			}

			// write the next frame
			const rgb_t *palette = (machine().first_screen() !=NULL && machine().first_screen()->palette() != NULL) ? machine().first_screen()->palette()->palette()->entry_list_adjusted() : NULL;
			int entries = (machine().first_screen() !=NULL && machine().first_screen()->palette() != NULL) ? machine().first_screen()->palette()->entries() : 0;
			png_error error = mng_capture_frame(*m_mng_file, &pnginfo, m_snap_bitmap, entries, palette);
			png_free(&pnginfo);
			if (error != PNGERR_NONE)
			{
				g_profiler.stop();
				end_recording(MF_MNG);
				break;
			}

			// advance time
			m_mng_next_frame_time += m_mng_frame_period;
			m_mng_frame++;
		}
	}

	g_profiler.stop();
}

//-------------------------------------------------
//  toggle_throttle
//-------------------------------------------------

void video_manager::toggle_throttle()
{
	set_throttled(!throttled());
}


//-------------------------------------------------
//  toggle_record_movie
//-------------------------------------------------

void video_manager::toggle_record_movie()
{
	if (!is_recording())
	{
		begin_recording(NULL, MF_WEBM);
		popmessage("REC START");
	}
	else
	{
		end_recording(MF_WEBM);
		popmessage("REC STOP");
	}
}
