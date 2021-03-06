/*
 * Licensed to GraphHopper GmbH under one or more contributor
 * license agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * GraphHopper GmbH licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graphhopper.timezone.webservice.resources;


import com.codahale.metrics.annotation.Timed;
import com.graphhopper.timezone.core.TimeZones;

import com.graphhopper.timezone.webservice.api.LocalTime;
import com.graphhopper.timezone.webservice.api.TimeZoneResponse;
import io.dropwizard.jersey.errors.ErrorMessage;

import javax.ws.rs.*;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("/timezone")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TimeZoneService {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TimeZoneService.class);

    private TimeZones timeZones;

    public TimeZoneService(TimeZones timeZoneReader) {
        this.timeZones = timeZoneReader;
    }

    @GET
    @Timed
    public Response handle(@Context UriInfo uriInfo){
        List<String> location = new ArrayList<>();
        if(uriInfo.getQueryParameters().containsKey("location")) {
            location = uriInfo.getQueryParameters().get("location");
            if (location.size() != 1)
                throwError(BAD_REQUEST.getStatusCode(), "only one location needs to be specified");
        }
        else throwError(BAD_REQUEST.getStatusCode(), "location missing. a location needs to be specified");;
        List<String> timestamps = new ArrayList<>();
        if(uriInfo.getQueryParameters().containsKey("timestamp")) {
            timestamps = uriInfo.getQueryParameters().get("timestamp");
            if (timestamps.size() != 1)
                throwError(BAD_REQUEST.getStatusCode(), "only one unix timestamp needs to be specified");
        }
        else throwError(BAD_REQUEST.getStatusCode(), "timestamp missing. unix timestamp needs to be specified");
        Locale locale = Locale.forLanguageTag("en");
        if(uriInfo.getQueryParameters().containsKey("language")) {
            List<String> languages = uriInfo.getQueryParameters().get("language");
            if (languages.size() != 1) throwError(BAD_REQUEST.getStatusCode(), "only one language needs to be specified");
            locale = Locale.forLanguageTag(languages.get(0));
        }

        String[] locationTokens = location.get(0).split(",");

        double lat = Double.parseDouble(locationTokens[0]);
        double lon = Double.parseDouble(locationTokens[1]);
        long timestamp = Long.parseLong(timestamps.get(0));

        String timeZoneId = timeZones.getTimeZone(lat,lon).getID();

        if(timeZoneId == null) {
            throwError(BAD_REQUEST.getStatusCode(),"could not localize location " + lat + ", " + lon);
        }

        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        OffsetDateTime localTime = timeZones.getOffsetDateTime(timestamp,timeZone);

        String displayName = timeZone.getDisplayName(locale);
        TimeZoneResponse timeZoneResponse = new TimeZoneResponse(timeZoneId, new LocalTime(localTime,locale), displayName);
        return Response.status(Response.Status.OK).entity(timeZoneResponse).build();
    }

    private void throwError(int statusCode, String msg){
        ErrorMessage errorMessage = new ErrorMessage(statusCode, msg);
        throw new WebApplicationException(errorMessage.getMessage(), Response.status(statusCode)
                .entity(errorMessage)
                .type(MediaType.APPLICATION_JSON).
                        build());
    }
}
